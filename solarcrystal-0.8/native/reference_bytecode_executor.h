#pragma once
#include "vm_value.h"
#include <cstddef>
#include <cstdint>
#include <functional>
#include <string>
#include <utility>
#include <vector>
namespace solarcrystal {
using NewClassResolver = std::function<std::string(std::uint16_t)>;
struct SpecialMethod { std::string owner_class; std::string name; std::string descriptor; std::vector<std::uint8_t> code; std::size_t max_locals=1; std::size_t max_stack=1; };
struct InstanceField {
 std::string owner_class;
 std::string name;
 std::string access_class;
 std::vector<std::string> compatible_runtime_classes;
 InstanceField() = default;
 InstanceField(std::string owner, std::string field_name)
  : owner_class(owner), name(std::move(field_name)), access_class(std::move(owner)),
    compatible_runtime_classes{access_class} {}
 InstanceField(std::string owner, std::string field_name, std::string access,
               std::vector<std::string> compatible)
  : owner_class(std::move(owner)), name(std::move(field_name)),
    access_class(std::move(access)), compatible_runtime_classes(std::move(compatible)) {}
};
using SpecialMethodResolver = std::function<SpecialMethod(std::uint16_t)>;
using InstanceFieldResolver = std::function<InstanceField(std::uint16_t)>;
struct ReferenceExecutionResult {
 std::size_t instructions_executed=0, objects_allocated=0, constructors_invoked=0, fields_written=0, fields_read=0;
 bool returned=false, has_return_value=false;
 VmValue return_value;
};
ReferenceExecutionResult execute_reference_bytecode(
 const std::vector<std::uint8_t>& code, VmFrame& frame, ObjectHeap& heap,
 const NewClassResolver& resolve_new_class,
 const SpecialMethodResolver& resolve_special_method={},
 const InstanceFieldResolver& resolve_instance_field={},
 std::size_t instruction_budget=100000);
}
