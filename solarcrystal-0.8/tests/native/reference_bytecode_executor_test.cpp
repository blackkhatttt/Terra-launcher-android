#include "reference_bytecode_executor.h"

#include <cassert>
#include <cstdint>
#include <iostream>
#include <stdexcept>
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

    // new #7; dup; astore_0; aload_0; astore_1
    const std::vector<std::uint8_t> code = {
        0xbb, 0x00, 0x07, 0x59, 0x4b, 0x2a, 0x4c
    };
    const auto result = execute_reference_bytecode(
        code, frame, heap,
        [](std::uint16_t index) {
            assert(index == 7);
            return std::string("SolarCrystalNode");
        });

    assert(result.instructions_executed == 5);
    assert(result.objects_allocated == 1);
    assert(heap.size() == 1);
    assert(frame.get_local(0).is_reference());
    assert(frame.get_local(1).is_reference());
    assert(frame.get_local(0).as_reference() == frame.get_local(1).as_reference());
    assert(heap.is_valid(frame.get_local(0).as_reference()));
    assert(frame.stack_size() == 0);

    // Explicit-index aload/astore and null references.
    VmFrame null_frame(2, 3);
    null_frame.set_local(0, VmValue::null_reference());
    const std::vector<std::uint8_t> null_code = {0x19, 0x00, 0x3a, 0x01};
    (void)execute_reference_bytecode(
        null_code, null_frame, heap,
        [](std::uint16_t) { return std::string(); });
    assert(null_frame.get_local(1).is_null());

    VmFrame invalid_frame(1, 2);
    invalid_frame.push(VmValue::integer(9));
    const std::vector<std::uint8_t> bad_store = {0x4b};
    assert(throws_runtime_error([&] {
        (void)execute_reference_bytecode(
            bad_store, invalid_frame, heap,
            [](std::uint16_t) { return std::string(); });
    }));

    const std::vector<std::uint8_t> truncated_new = {0xbb, 0x00};
    assert(throws_runtime_error([&] {
        VmFrame f(1, 2);
        (void)execute_reference_bytecode(
            truncated_new, f, heap,
            [](std::uint16_t) { return std::string("X"); });
    }));

    const std::vector<std::uint8_t> unsupported = {0x00};
    assert(throws_runtime_error([&] {
        VmFrame f(1, 2);
        (void)execute_reference_bytecode(
            unsupported, f, heap,
            [](std::uint16_t) { return std::string(); });
    }));

    assert(throws_runtime_error([&] {
        VmFrame f(1, 2);
        (void)execute_reference_bytecode(
            {0xbb, 0x00, 0x01}, f, heap,
            [](std::uint16_t) { return std::string("X"); }, 1);
    }));

    std::cout << "SolarCrystal reference bytecodes: PASS\n";
    std::cout << "aload/astore, null, dup, pop, new and bounds: PASS\n";
    return 0;
}
