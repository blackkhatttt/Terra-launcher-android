#include "reference_bytecode_executor.h"

#include <cassert>
#include <cstdint>
#include <iostream>
#include <stdexcept>
#include <string>
#include <vector>

using namespace solarcrystal;

template <typename Function>
static bool throws_runtime_error(Function function) {
    try { function(); }
    catch (const std::runtime_error&) { return true; }
    return false;
}

int main() {
    ObjectHeap heap;
    VmFrame frame(4, 8);
    const std::vector<std::uint8_t> allocation = {
        0xbb, 0x00, 0x07, 0x59, 0x4b, 0x2a, 0x4c
    };
    const auto basic = execute_reference_bytecode(
        allocation, frame, heap,
        [](std::uint16_t index) {
            assert(index == 7);
            return std::string("SolarCrystalNode");
        });
    assert(basic.instructions_executed == 5);
    assert(basic.objects_allocated == 1);
    assert(frame.get_local(0).as_reference() == frame.get_local(1).as_reference());
    assert(frame.pop().as_reference() == frame.get_local(0).as_reference());

    // new Node; dup; invokespecial Node.<init>()V; astore_0
    // Constructor: this.value = 42; return
    VmFrame constructor_caller(1, 4);
    const std::vector<std::uint8_t> call_constructor = {
        0xbb, 0x00, 0x07, 0x59, 0xb7, 0x00, 0x09, 0x4b
    };
    const std::vector<std::uint8_t> constructor_code = {
        0x2a, 0x10, 0x2a, 0xb5, 0x00, 0x0b, 0xb1
    };
    const auto constructed = execute_reference_bytecode(
        call_constructor, constructor_caller, heap,
        [](std::uint16_t index) {
            assert(index == 7);
            return std::string("SolarCrystalNode");
        },
        [&](std::uint16_t index) {
            assert(index == 9);
            return SpecialMethod{"SolarCrystalNode", "<init>", "()V",
                                 constructor_code, 1, 3};
        },
        [](std::uint16_t index) {
            assert(index == 11);
            return InstanceField{"SolarCrystalNode", "value"};
        });
    assert(constructed.objects_allocated == 1);
    assert(constructed.constructors_invoked == 1);
    assert(constructed.fields_written == 1);
    const ObjectId node = constructor_caller.get_local(0).as_reference();
    assert(heap.get_int(node, "SolarCrystalNode", "value") == 42);

    // A separate getfield path reads the value set by the constructor.
    VmFrame field_reader(1, 2);
    field_reader.set_local(0, VmValue::reference(node));
    const auto read = execute_reference_bytecode(
        {0x2a, 0xb4, 0x00, 0x0c}, field_reader, heap,
        [](std::uint16_t) { return std::string(); }, {},
        [](std::uint16_t index) {
            assert(index == 12);
            return InstanceField{"SolarCrystalNode", "value"};
        });
    assert(read.fields_read == 1);
    assert(field_reader.pop().as_integer() == 42);

    // java/lang/Object.<init>()V is treated as the intrinsic no-op base constructor.
    VmFrame object_base(1, 3);
    const auto base_result = execute_reference_bytecode(
        {0xbb, 0x00, 0x01, 0x59, 0xb7, 0x00, 0x02, 0x4b},
        object_base, heap,
        [](std::uint16_t) { return std::string("Example"); },
        [](std::uint16_t) {
            return SpecialMethod{"java/lang/Object", "<init>", "()V", {}, 1, 1};
        });
    assert(base_result.constructors_invoked == 1);

    VmFrame null_frame(2, 3);
    null_frame.set_local(0, VmValue::null_reference());
    (void)execute_reference_bytecode(
        {0x19, 0x00, 0x3a, 0x01}, null_frame, heap,
        [](std::uint16_t) { return std::string(); });
    assert(null_frame.get_local(1).is_null());

    assert(throws_runtime_error([&] {
        VmFrame f(1, 2);
        (void)execute_reference_bytecode(
            {0xbb, 0x00, 0x01, 0x57}, f, heap,
            [](std::uint16_t) { return std::string("X"); }, {}, {}, 1);
    }));
    assert(throws_runtime_error([&] {
        VmFrame f(1, 2);
        (void)execute_reference_bytecode(
            {0xbb, 0x00, 0x01, 0x59, 0xb7, 0x00, 0x02}, f, heap,
            [](std::uint16_t) { return std::string("X"); },
            [](std::uint16_t) {
                return SpecialMethod{"OtherClass", "<init>", "()V", {}, 1, 1};
            });
    }));

    std::cout << "SolarCrystal constructor bytecodes: PASS\n";
    std::cout << "invokespecial, <init>(), putfield/getfield and Object constructor: PASS\n";
    return 0;
}
