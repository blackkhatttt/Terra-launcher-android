#include "reference_bytecode_executor.h"

#include <stdexcept>
#include <utility>

namespace solarcrystal {
namespace {

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

void store_reference(VmFrame& frame, std::size_t index, const VmValue& value, const ObjectHeap& heap) {
    if (!value.is_reference() && !value.is_null())
        throw std::runtime_error("astore requires a reference or null value");
    if (value.is_reference() && !heap.is_valid(value.as_reference()))
        throw std::runtime_error("astore encountered an invalid object reference");
    frame.set_local(index, value);
}

} // namespace

ReferenceExecutionResult execute_reference_bytecode(
    const std::vector<std::uint8_t>& code,
    VmFrame& frame,
    ObjectHeap& heap,
    const NewClassResolver& resolve_new_class,
    std::size_t instruction_budget) {
    if (instruction_budget == 0)
        throw std::runtime_error("Reference bytecode instruction budget is zero");

    ReferenceExecutionResult result;
    std::size_t pc = 0;

    while (pc < code.size()) {
        if (++result.instructions_executed > instruction_budget)
            throw std::runtime_error("Reference bytecode instruction budget exceeded");

        const std::uint8_t opcode = read_u1(code, pc);
        switch (opcode) {
            case 0x19: // aload
                frame.push(load_reference(frame, read_u1(code, pc), heap));
                break;

            case 0x2a: case 0x2b: case 0x2c: case 0x2d: // aload_0..aload_3
                frame.push(load_reference(frame, opcode - 0x2a, heap));
                break;

            case 0x3a: // astore
                {
                    const auto index = read_u1(code, pc);
                    const auto value = frame.pop();
                    store_reference(frame, index, value, heap);
                }
                break;

            case 0x4b: case 0x4c: case 0x4d: case 0x4e: // astore_0..astore_3
                {
                    const auto value = frame.pop();
                    store_reference(frame, opcode - 0x4b, value, heap);
                }
                break;

            case 0x59: // dup
                frame.duplicate_top();
                break;

            case 0x57: // pop
                (void)frame.pop();
                break;

            case 0xbb: // new
                {
                    const auto cp_index = read_u2(code, pc);
                    if (!resolve_new_class)
                        throw std::runtime_error("new has no class constant resolver");
                    const std::string class_name = resolve_new_class(cp_index);
                    if (class_name.empty())
                        throw std::runtime_error("new resolved an empty class name");
                    const ObjectId id = heap.allocate(class_name, {});
                    frame.push(VmValue::reference(id));
                    ++result.objects_allocated;
                }
                break;

            default:
                throw std::runtime_error("Unsupported reference bytecode opcode: " +
                                         std::to_string(opcode));
        }
    }

    return result;
}

} // namespace solarcrystal
