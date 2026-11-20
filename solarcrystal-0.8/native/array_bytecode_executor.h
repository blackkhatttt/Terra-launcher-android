#pragma once
#include "vm_array_heap.h"
#include <cstddef>
#include <cstdint>
#include <functional>
#include <string>
#include <vector>
namespace solarcrystal {
using NewArrayClassResolver=std::function<std::string(std::uint16_t)>;
struct ArrayExecutionResult { std::size_t instructions_executed=0,arrays_allocated=0,elements_read=0,elements_written=0; };
ArrayExecutionResult execute_array_bytecode(const std::vector<std::uint8_t>& code,VmFrame& frame,VmArrayHeap& arrays,const NewArrayClassResolver& resolve_class={},std::size_t instruction_budget=100000);
}