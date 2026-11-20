#include "class_file_method_executor.h"

#include <stdexcept>
#include <string>
#include <vector>

namespace solarcrystal {
namespace {

struct MethodDescriptor {
    std::vector<std::string> parameters;
    std::string return_type;
};

std::string parse_type(const std::string& descriptor, std::size_t& position,
                       bool allow_void) {
    if (position >= descriptor.size()) {
        throw std::runtime_error("Truncated method descriptor");
    }
    const auto start = position;
    const char type = descriptor[position++];
    if (type == 'I') return "I";
    if (type == 'V' && allow_void) return "V";
    if (type == 'L') {
        const auto end = descriptor.find(';', position);
        if (end == std::string::npos || end == position) {
            throw std::runtime_error("Malformed reference type in method descriptor");
        }
        const auto name = descriptor.substr(position, end - position);
        if (name.find('.') != std::string::npos || name.front() == '[') {
            throw std::runtime_error("Invalid reference type in method descriptor");
        }
        position = end + 1;
        return descriptor.substr(start, position - start);
    }
    throw std::runtime_error("Unsupported method descriptor type");
}

MethodDescriptor parse_method_descriptor(const std::string& descriptor) {
    if (descriptor.empty() || descriptor.front() != '(') {
        throw std::runtime_error("Malformed method descriptor");
    }
    MethodDescriptor parsed;
    std::size_t position = 1;
    while (position < descriptor.size() && descriptor[position] != ')') {
        parsed.parameters.push_back(parse_type(descriptor, position, false));
    }
    if (position >= descriptor.size() || descriptor[position] != ')') {
        throw std::runtime_error("Method descriptor is missing closing parenthesis");
    }
    ++position;
    parsed.return_type = parse_type(descriptor, position, true);
    if (position != descriptor.size()) {
        throw std::runtime_error("Trailing data in method descriptor");
    }
    return parsed;
}

void validate_argument(const VmValue& value, const std::string& type,
                       const ClassFileRepository& repository,
                       const ObjectHeap& heap) {
    if (type == "I") {
        if (!value.is_integer()) {
            throw std::runtime_error("Method argument type mismatch: expected integer");
        }
        return;
    }
    const auto expected_class = type.substr(1, type.size() - 2);
    if (value.is_null()) return;
    if (!value.is_reference() || !heap.is_valid(value.as_reference())) {
        throw std::runtime_error("Method argument type mismatch: expected object reference");
    }
    if (!repository.find(expected_class)) {
        throw std::runtime_error("Method parameter class is not registered: " + expected_class);
    }
    if (heap.class_name(value.as_reference()) != expected_class) {
        throw std::runtime_error("Method argument reference class mismatch");
    }
}

} // namespace

ReferenceExecutionResult execute_class_method(
    const ClassFileModel& model,
    const ClassFileRepository& repository,
    const std::string& method_name,
    const std::string& descriptor,
    ObjectHeap& heap,
    const std::vector<VmValue>& arguments,
    const VmValue* receiver,
    std::size_t instruction_budget) {
    if (repository.find(model.name()) != &model) {
        throw std::runtime_error("Method class is not registered in repository");
    }
    const auto* method = model.find_method(method_name, descriptor);
    if (!method) {
        throw std::runtime_error("Method not found: " + model.name() + "." +
                                 method_name + descriptor);
    }
    if (!method->has_code) {
        throw std::runtime_error("Method has no executable Code attribute");
    }
    const auto parsed = parse_method_descriptor(descriptor);
    if (arguments.size() != parsed.parameters.size()) {
        throw std::runtime_error("Method argument count mismatch");
    }

    constexpr std::uint16_t kAccStatic = 0x0008;
    const bool is_static = (method->access_flags & kAccStatic) != 0;
    if (is_static && receiver) {
        throw std::runtime_error("Static method must not receive an instance");
    }
    if (!is_static && !receiver) {
        throw std::runtime_error("Instance method requires a receiver");
    }

    VmFrame frame(method->max_locals, method->max_stack);
    std::size_t local_index = 0;
    if (!is_static) {
        if (!receiver->is_reference() || !heap.is_valid(receiver->as_reference())) {
            throw std::runtime_error("Instance method requires a valid non-null receiver");
        }
        if (heap.class_name(receiver->as_reference()) != model.name()) {
            throw std::runtime_error("Instance method receiver class mismatch");
        }
        frame.set_local(local_index++, *receiver);
    }
    for (std::size_t i = 0; i < arguments.size(); ++i) {
        validate_argument(arguments[i], parsed.parameters[i], repository, heap);
        if (local_index >= frame.local_count()) {
            throw std::runtime_error("Method arguments exceed max_locals");
        }
        frame.set_local(local_index++, arguments[i]);
    }

    ClassFileExecutionResolver resolver(model, repository);
    const auto result = execute_reference_bytecode(
        method->code, frame, heap, resolver.new_class_resolver(),
        resolver.special_method_resolver(), resolver.instance_field_resolver(),
        instruction_budget);
    if (!result.returned) {
        throw std::runtime_error("Method bytecode completed without a return instruction");
    }
    if (parsed.return_type == "I" && !result.has_return_value) {
        throw std::runtime_error("Integer method did not return an integer value");
    }
    if (parsed.return_type == "V" && result.has_return_value) {
        throw std::runtime_error("Void method unexpectedly returned a value");
    }
    return result;
}

} // namespace solarcrystal
