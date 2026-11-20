#include "class_file_execution_resolver.h"

#include <cassert>
#include <cstdint>
#include <fstream>
#include <iostream>
#include <iterator>
#include <stdexcept>
#include <vector>

using namespace solarcrystal;

int main(int argc, char** argv) {
    assert(argc == 2);
    std::ifstream input(argv[1], std::ios::binary);
    assert(input.good());
    const std::vector<std::uint8_t> bytes(
        (std::istreambuf_iterator<char>(input)),
        std::istreambuf_iterator<char>());
    assert(!bytes.empty());

    const auto model = ClassFileModel::parse(bytes.data(), bytes.size());
    assert(model.name() == "SolarCrystalObjectFixture");

    ClassFileExecutionResolver resolver(model);
    const auto new_class = resolver.new_class_resolver();
    const auto special = resolver.special_method_resolver();
    const auto field = resolver.instance_field_resolver();

    const auto* constructor = model.find_method("<init>", "()V");
    const auto* writer = model.find_method("writeValue", "(I)V");
    const auto* reader = model.find_method("readValue", "()I");
    assert(constructor && constructor->has_code);
    assert(writer && writer->has_code);
    assert(reader && reader->has_code);

    ObjectHeap heap;
    // Find the actual CONSTANT_Class entry and the Object.<init> Methodref
    // in the parsed class rather than assuming compiler-assigned indexes.
    std::uint16_t class_index = 0;
    std::uint16_t object_constructor_index = 0;
    for (std::uint32_t i = 1; i < model.constant_pool().size(); ++i) {
        const auto tag = model.constant_pool()[i].tag;
        if (tag == 7 && model.class_name_at(static_cast<std::uint16_t>(i)) == model.name()) {
            class_index = static_cast<std::uint16_t>(i);
        } else if (tag == 10 || tag == 11) {
            const auto ref = model.member_reference_at(static_cast<std::uint16_t>(i));
            if (ref.owner == "java/lang/Object" && ref.name == "<init>" &&
                ref.descriptor == "()V") {
                object_constructor_index = static_cast<std::uint16_t>(i);
            }
        }
    }
    assert(class_index != 0);
    assert(object_constructor_index != 0);

    std::vector<std::uint8_t> code = {
        0xbb, static_cast<std::uint8_t>(class_index >> 8),
        static_cast<std::uint8_t>(class_index & 0xff),
        0x59,
        0xb7, static_cast<std::uint8_t>(object_constructor_index >> 8),
        static_cast<std::uint8_t>(object_constructor_index & 0xff),
        0x4b
    };
    VmFrame allocation_frame(2, 4);
    const auto allocated = execute_reference_bytecode(
        code, allocation_frame, heap, new_class, special, field);
    assert(allocated.objects_allocated == 1);
    assert(allocated.constructors_invoked == 1);
    const auto object_id = allocation_frame.get_local(0).as_reference();
    assert(heap.class_name(object_id) == model.name());
    assert(heap.get_int(object_id, model.name(), "value") == 42);

    // Execute the real writeValue(int) bytecode using its parsed Code attribute.
    VmFrame write_frame(writer->max_locals, writer->max_stack);
    write_frame.set_local(0, VmValue::reference(object_id));
    write_frame.set_local(1, VmValue::integer(73));
    const auto written = execute_reference_bytecode(
        writer->code, write_frame, heap, new_class, special, field);
    assert(written.returned);
    assert(written.fields_written == 1);
    assert(heap.get_int(object_id, model.name(), "value") == 73);

    // Execute the real readValue() bytecode and verify its typed return value.
    VmFrame read_frame(reader->max_locals, reader->max_stack);
    read_frame.set_local(0, VmValue::reference(object_id));
    const auto read = execute_reference_bytecode(
        reader->code, read_frame, heap, new_class, special, field);
    assert(read.returned && read.has_return_value);
    assert(read.return_value.is_integer());
    assert(read.return_value.as_integer() == 73);
    assert(read.fields_read == 1);

    bool rejected = false;
    try {
        (void)resolver.instance_field_resolver()(object_constructor_index);
    } catch (const std::runtime_error&) {
        rejected = true;
    }
    assert(rejected);

    std::cout << "SolarCrystal 0.8.6b Constant Pool execution resolver: PASS\n";
    std::cout << "real .class constructor, putfield and getfield execution: PASS\n";
}
