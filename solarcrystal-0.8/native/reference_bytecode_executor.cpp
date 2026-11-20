#include "reference_bytecode_executor.h"

#include <stdexcept>
#include <string>
#include <utility>

namespace solarcrystal {
namespace {

struct ExecutionContext {
    ObjectHeap& heap;
    const NewClassResolver& resolve_new_class;
    const SpecialMethodResolver& resolve_special_method;
    const InstanceFieldResolver& resolve_instance_field;
    std::size_t budget;
    ReferenceExecutionResult result;
    std::size_t constructor_depth = 0;
};

std::uint8_t read_u1(const std::vector<std::uint8_t>& code, std::size_t& pc) {
    if (pc >= code.size())
        throw std::runtime_error("Truncated bytecode operand");
    return code[pc++];
}

std::uint16_t read_u2(const std::vector<std::uint8_t>& code, std::size_t& pc) {
    const auto high = read_u1(code, pc);
    const auto low = read_u1(code, pc);
    return static_cast<std::uint16_t>((high << 8) | low);
}

VmValue load_reference(const VmFrame& frame, std::size_t index, const ObjectHeap& heap) {
    const VmValue& value = frame.get_local(index);
    if (!value.is_reference() && !value.is_null())
        throw std::runtime_error("aload requires a reference or null local");
    if (value.is_reference() && !heap.is_valid(value.as_reference()))
        throw std::runtime_error("aload encountered an invalid object reference");
    return value;
}

void store_reference(VmFrame& frame, std::size_t index,
                     const VmValue& value, const ObjectHeap& heap) {
    if (!value.is_reference() && !value.is_null())
        throw std::runtime_error("astore requires a reference or null value");
    if (value.is_reference() && !heap.is_valid(value.as_reference()))
        throw std::runtime_error("astore encountered an invalid object reference");
    frame.set_local(index, value);
}

ObjectId pop_object_reference(VmFrame& frame, const ObjectHeap& heap) {
    const VmValue value = frame.pop();
    if (!value.is_reference())
        throw std::runtime_error("Instance operation requires a non-null object reference");
    const ObjectId id = value.as_reference();
    if (!heap.is_valid(id))
        throw std::runtime_error("Instance operation encountered an invalid object reference");
    return id;
}

void execute_code(const std::vector<std::uint8_t>& code,
                  VmFrame& frame,
                  ExecutionContext& context,
                  bool constructor_body);

void invoke_constructor(std::uint16_t cp_index, VmFrame& caller,
                        ExecutionContext& context) {
    if (!context.resolve_special_method)
        throw std::runtime_error("invokespecial has no method resolver");
    const SpecialMethod method = context.resolve_special_method(cp_index);
    if (method.name != "<init>" || method.descriptor != "()V")
        throw std::runtime_error("Only no-argument void constructors are supported");

    const ObjectId receiver = pop_object_reference(caller, context.heap);
    const std::string actual_class = context.heap.class_name(receiver);
    if (method.owner_class == "java/lang/Object") {
        ++context.result.constructors_invoked;
        return;
    }
    if (method.owner_class != actual_class)
        throw std::runtime_error("Superclass constructor dispatch is not supported yet");
    if (context.constructor_depth >= 16)
        throw std::runtime_error("Constructor call depth limit exceeded");

    VmFrame constructor_frame(method.max_locals, method.max_stack);
    constructor_frame.set_local(0, VmValue::reference(receiver));
    ++context.constructor_depth;
    ++context.result.constructors_invoked;
    try {
        execute_code(method.code, constructor_frame, context, true);
    } catch (...) {
        --context.constructor_depth;
        throw;
    }
    --context.constructor_depth;
}

void execute_code(const std::vector<std::uint8_t>& code,
                  VmFrame& frame,
                  ExecutionContext& context,
                  bool constructor_body) {
    std::size_t pc = 0;
    bool returned = false;
    while (pc < code.size()) {
        if (++context.result.instructions_executed > context.budget)
            throw std::runtime_error("Reference bytecode instruction budget exceeded");

        const std::uint8_t opcode = read_u1(code, pc);
        switch (opcode) {
            case 0x19: frame.push(load_reference(frame, read_u1(code, pc), context.heap)); break;
            case 0x2a: case 0x2b: case 0x2c: case 0x2d:
                frame.push(load_reference(frame, opcode - 0x2a, context.heap)); break;
            case 0x3a: {
                const auto index = read_u1(code, pc);
                const auto value = frame.pop();
                store_reference(frame, index, value, context.heap);
                break;
            }
            case 0x4b: case 0x4c: case 0x4d: case 0x4e: {
                const auto value = frame.pop();
                store_reference(frame, opcode - 0x4b, value, context.heap);
                break;
            }
            case 0x59: frame.duplicate_top(); break;
            case 0x57: (void)frame.pop(); break;
            case 0xbb: {
                const auto cp_index = read_u2(code, pc);
                if (!context.resolve_new_class)
                    throw std::runtime_error("new has no class constant resolver");
                const std::string class_name = context.resolve_new_class(cp_index);
                if (class_name.empty())
                    throw std::runtime_error("new resolved an empty class name");
                const ObjectId id = context.heap.allocate(class_name, {});
                frame.push(VmValue::reference(id));
                ++context.result.objects_allocated;
                break;
            }
            case 0xb7: invoke_constructor(read_u2(code, pc), frame, context); break;
            case 0xb5: {
                const auto cp_index = read_u2(code, pc);
                if (!context.resolve_instance_field)
                    throw std::runtime_error("putfield has no field resolver");
                const InstanceField field = context.resolve_instance_field(cp_index);
                const VmValue value = frame.pop();
                if (!value.is_integer())
                    throw std::runtime_error("Only integer putfield values are supported");
                const ObjectId receiver = pop_object_reference(frame, context.heap);
                if (field.owner_class != context.heap.class_name(receiver))
                    throw std::runtime_error("putfield owner does not match object class");
                context.heap.define_int_field(receiver, field.name, 0);
                context.heap.put_int(receiver, field.owner_class, field.name, value.as_integer());
                ++context.result.fields_written;
                break;
            }
            case 0xb4: {
                const auto cp_index = read_u2(code, pc);
                if (!context.resolve_instance_field)
                    throw std::runtime_error("getfield has no field resolver");
                const InstanceField field = context.resolve_instance_field(cp_index);
                const ObjectId receiver = pop_object_reference(frame, context.heap);
                if (field.owner_class != context.heap.class_name(receiver))
                    throw std::runtime_error("getfield owner does not match object class");
                frame.push(VmValue::integer(
                    context.heap.get_int(receiver, field.owner_class, field.name)));
                ++context.result.fields_read;
                break;
            }
            case 0x10: frame.push(VmValue::integer(static_cast<std::int8_t>(read_u1(code, pc)))); break;
            case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07: case 0x08:
                frame.push(VmValue::integer(static_cast<std::int32_t>(opcode) - 0x03)); break;
            case 0x60: {
                const auto right = frame.pop();
                const auto left = frame.pop();
                frame.push(VmValue::integer(left.as_integer() + right.as_integer()));
                break;
            }
            case 0xb1:
                if (!constructor_body)
                    throw std::runtime_error("return outside constructor execution");
                returned = true;
                if (pc != code.size())
                    throw std::runtime_error("Instructions after constructor return are unsupported");
                break;
            default:
                throw std::runtime_error("Unsupported reference bytecode opcode: " +
                                         std::to_string(opcode));
        }
        if (returned) break;
    }
}

} // namespace

ReferenceExecutionResult execute_reference_bytecode(
    const std::vector<std::uint8_t>& code,
    VmFrame& frame,
    ObjectHeap& heap,
    const NewClassResolver& resolve_new_class,
    const SpecialMethodResolver& resolve_special_method,
    const InstanceFieldResolver& resolve_instance_field,
    std::size_t instruction_budget) {
    if (instruction_budget == 0)
        throw std::runtime_error("Reference bytecode instruction budget is zero");
    ExecutionContext context{heap, resolve_new_class, resolve_special_method,
                             resolve_instance_field, instruction_budget, {}, 0};
    execute_code(code, frame, context, false);
    return context.result;
}

} // namespace solarcrystal
