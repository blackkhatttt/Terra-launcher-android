#include "class_file_execution_resolver.h"

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
    if (!input.good()) {
        throw std::runtime_error(std::string("Cannot open class file: ") + path);
    }
    const std::vector<std::uint8_t> bytes(
        (std::istreambuf_iterator<char>(input)),
        std::istreambuf_iterator<char>());
    if (bytes.empty()) {
        throw std::runtime_error("Class file is empty");
    }
    return ClassFileModel::parse(bytes.data(), bytes.size());
}

int main(int argc, char** argv) {
    assert(argc == 3);
    const auto primary = load_model(argv[1]);
    const auto secondary = load_model(argv[2]);
    assert(primary.name() == "SolarCrystalObjectFixture");
    assert(secondary.name() == "SolarCrystalSecondaryFixture");

    ClassFileRepository repository;
    repository.add(primary);
    repository.add(secondary);
    assert(repository.size() == 2);
    assert(repository.find(primary.name()) == &primary);
    assert(repository.find(secondary.name()) == &secondary);
    assert(repository.find("missing/Type") == nullptr);

    bool duplicate_rejected = false;
    try {
        repository.add(primary);
    } catch (const std::runtime_error&) {
        duplicate_rejected = true;
    }
    assert(duplicate_rejected);

    ClassFileExecutionResolver secondary_resolver(secondary, repository);
    const auto resolve_class = secondary_resolver.new_class_resolver();
    const auto resolve_special = secondary_resolver.special_method_resolver();
    const auto resolve_field = secondary_resolver.instance_field_resolver();

    std::uint16_t primary_class_index = 0;
    std::uint16_t primary_constructor_index = 0;
    std::uint16_t primary_value_field_index = 0;
    for (std::uint32_t i = 1; i < secondary.constant_pool().size(); ++i) {
        const auto tag = secondary.constant_pool()[i].tag;
        const auto index = static_cast<std::uint16_t>(i);
        if (tag == 7 && secondary.class_name_at(index) == primary.name()) {
            primary_class_index = index;
        } else if (tag == 10 || tag == 11) {
            const auto ref = secondary.member_reference_at(index);
            if (ref.owner == primary.name() && ref.name == "<init>" &&
                ref.descriptor == "()V") {
                primary_constructor_index = index;
            }
        } else if (tag == 9) {
            const auto ref = secondary.member_reference_at(index);
            if (ref.owner == primary.name() && ref.name == "value" &&
                ref.descriptor == "I") {
                primary_value_field_index = index;
            }
        }
    }
    assert(primary_class_index != 0);
    assert(primary_constructor_index != 0);
    assert(primary_value_field_index != 0);
    assert(resolve_class(primary_class_index) == primary.name());

    const auto constructor = resolve_special(primary_constructor_index);
    assert(constructor.owner_class == primary.name());
    assert(constructor.name == "<init>");
    assert(!constructor.code.empty());

    const auto field = resolve_field(primary_value_field_index);
    assert(field.owner_class == primary.name());
    assert(field.name == "value");

    ClassFileExecutionResolver primary_resolver(primary, repository);
    std::uint16_t own_class_index = 0;
    for (std::uint32_t i = 1; i < primary.constant_pool().size(); ++i) {
        const auto index = static_cast<std::uint16_t>(i);
        if (primary.constant_pool()[i].tag == 7 &&
            primary.class_name_at(index) == primary.name()) {
            own_class_index = index;
            break;
        }
    }
    assert(own_class_index != 0);

    ObjectHeap heap;
    const std::vector<std::uint8_t> allocation_code = {
        0xbb, static_cast<std::uint8_t>(own_class_index >> 8),
        static_cast<std::uint8_t>(own_class_index & 0xff), 0x59, 0x4b
    };
    VmFrame allocation_frame(2, 4);
    const auto allocation = execute_reference_bytecode(
        allocation_code, allocation_frame, heap,
        primary_resolver.new_class_resolver(),
        primary_resolver.special_method_resolver(),
        primary_resolver.instance_field_resolver());
    assert(allocation.objects_allocated == 1);
    const auto object_id = allocation_frame.get_local(0).as_reference();

    const auto* primary_ctor = primary.find_method("<init>", "()V");
    assert(primary_ctor && primary_ctor->has_code);
    VmFrame ctor_frame(primary_ctor->max_locals, primary_ctor->max_stack);
    ctor_frame.set_local(0, VmValue::reference(object_id));
    const auto initialized = execute_reference_bytecode(
        primary_ctor->code, ctor_frame, heap,
        primary_resolver.new_class_resolver(),
        primary_resolver.special_method_resolver(),
        primary_resolver.instance_field_resolver());
    assert(initialized.returned);
    assert(heap.get_int(object_id, primary.name(), "value") == 42);

    const auto* reader = secondary.find_method(
        "readPrimaryValue", "(LSolarCrystalObjectFixture;)I");
    const auto* writer = secondary.find_method(
        "writePrimaryValue", "(LSolarCrystalObjectFixture;I)V");
    assert(reader && reader->has_code);
    assert(writer && writer->has_code);

    VmFrame write_frame(writer->max_locals, writer->max_stack);
    write_frame.set_local(0, VmValue::null_reference());
    write_frame.set_local(1, VmValue::reference(object_id));
    write_frame.set_local(2, VmValue::integer(88));
    const auto write_result = execute_reference_bytecode(
        writer->code, write_frame, heap, resolve_class, resolve_special, resolve_field);
    assert(write_result.returned);
    assert(write_result.fields_written == 1);
    assert(heap.get_int(object_id, primary.name(), "value") == 88);

    VmFrame read_frame(reader->max_locals, reader->max_stack);
    read_frame.set_local(0, VmValue::null_reference());
    read_frame.set_local(1, VmValue::reference(object_id));
    const auto read_result = execute_reference_bytecode(
        reader->code, read_frame, heap, resolve_class, resolve_special, resolve_field);
    assert(read_result.returned && read_result.has_return_value);
    assert(read_result.return_value.as_integer() == 88);
    assert(read_result.fields_read == 1);

    std::cout << "SolarCrystal 0.8.6c multi-class repository: PASS\n";
    std::cout << "cross-class constructor and field reference resolution: PASS\n";
}
