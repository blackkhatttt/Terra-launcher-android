#pragma once

#include "class_file_execution_resolver.h"

#include <cstddef>
#include <string>
#include <vector>

namespace solarcrystal {

ReferenceExecutionResult execute_class_method(
    const ClassFileModel& model,
    const ClassFileRepository& repository,
    const std::string& method_name,
    const std::string& descriptor,
    ObjectHeap& heap,
    const std::vector<VmValue>& arguments = {},
    const VmValue* receiver = nullptr,
    std::size_t instruction_budget = 100000);

} // namespace solarcrystal
