#pragma once

#include "vm_value.h"

#include <cstddef>
#include <cstdint>
#include <functional>
#include <string>
#include <vector>

namespace solarcrystal {

using NewClassResolver = std::function<std::string(std::uint16_t)>;

struct ReferenceExecutionResult {
    std::size_t instructions_executed = 0;
    std::size_t objects_allocated = 0;
};

// Executes the first bounded reference-opcode subset against an existing frame.
// Supported: aload, aload_0..3, astore, astore_0..3, dup, pop, new.
ReferenceExecutionResult execute_reference_bytecode(
    const std::vector<std::uint8_t>& code,
    VmFrame& frame,
    ObjectHeap& heap,
    const NewClassResolver& resolve_new_class,
    std::size_t instruction_budget = 100000);

} // namespace solarcrystal
