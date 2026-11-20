#include "class_file_method_executor.h"

#include <cassert>
#include <cstdint>
#include <fstream>
#include <iostream>
#include <iterator>
#include <stdexcept>
#include <string>
#include <vector>

using namespace solarcrystal;

static ClassFileModel load_model(const char* path) {
    std::ifstream input(path, std::ios::binary);
    if (!input.good()) throw std::runtime_error(std::string("Cannot open class file: ") + path);
    const std::vector<std::uint8_t> bytes((std::istreambuf_iterator<char>(input)),
                                          std::istreambuf_iterator<char>());
    if (bytes.empty()) throw std::runtime_error("Class file is empty");
    return ClassFileModel::parse(bytes.data(), bytes.size());
}

int main(int argc, char** argv) {
    assert(argc == 3);
    const auto primary = load_model(argv[1]);
    const auto secondary = load_model(argv[2]);
    ClassFileRepository repository;
    repository.add(primary);
    repository.add(secondary);

    ObjectHeap heap;
    const auto secondary_id = heap.allocate(secondary.name(), {});
    const auto secondary_receiver = VmValue::reference(secondary_id);
    const auto constructor = execute_class_method(
        secondary, repository, "<init>", "()V", heap, {}, &secondary_receiver);
    assert(constructor.returned && !constructor.has_return_value);
    assert(heap.get_int(secondary_id, secondary.name(), "value") == 19);

    const auto primary_id = heap.allocate(primary.name(), {});
    heap.define_int_field(primary_id, "value", 42);
    const auto primary_argument = VmValue::reference(primary_id);
    const auto read = execute_class_method(
        secondary, repository, "readPrimaryValue", "(LSolarCrystalObjectFixture;)I",
        heap, {primary_argument}, &secondary_receiver);
    assert(read.returned && read.has_return_value);
    assert(read.return_value.as_integer() == 42 && read.fields_read == 1);

    const auto write = execute_class_method(
        secondary, repository, "writePrimaryValue", "(LSolarCrystalObjectFixture;I)V",
        heap, {primary_argument, VmValue::integer(73)}, &secondary_receiver);
    assert(write.returned && !write.has_return_value);
    assert(write.fields_written == 1);
    assert(heap.get_int(primary_id, primary.name(), "value") == 73);

    const auto sum = execute_class_method(
        secondary, repository, "sum", "(II)I", heap,
        {VmValue::integer(20), VmValue::integer(22)});
    assert(sum.returned && sum.has_return_value);
    assert(sum.return_value.as_integer() == 42);

    bool missing_method_rejected = false;
    try {
        (void)execute_class_method(secondary, repository, "doesNotExist", "()I", heap);
    } catch (const std::runtime_error&) { missing_method_rejected = true; }
    assert(missing_method_rejected);

    bool wrong_argument_rejected = false;
    try {
        (void)execute_class_method(secondary, repository, "sum", "(II)I", heap,
                                   {VmValue::integer(1)});
    } catch (const std::runtime_error&) { wrong_argument_rejected = true; }
    assert(wrong_argument_rejected);

    std::cout << "SolarCrystal 0.8.6d parsed method execution: PASS\n";
    std::cout << "instance, static, constructor, and cross-class field methods: PASS\n";
}
