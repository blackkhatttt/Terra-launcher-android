#include "vm_value.h"

#include <stdexcept>

namespace solarcrystal {

VmValue VmValue::integer(std::int32_t value) {
    VmValue result;
    result.kind_ = VmValueKind::Integer;
    result.integer_ = value;
    return result;
}

VmValue VmValue::reference(ObjectId object_id) {
    if (object_id == 0)
        throw std::runtime_error("A null object ID must use null_reference()");
    VmValue result;
    result.kind_ = VmValueKind::Reference;
    result.reference_ = object_id;
    return result;
}

VmValue VmValue::null_reference() {
    VmValue result;
    result.kind_ = VmValueKind::Null;
    return result;
}

VmValueKind VmValue::kind() const { return kind_; }
bool VmValue::is_integer() const { return kind_ == VmValueKind::Integer; }
bool VmValue::is_reference() const { return kind_ == VmValueKind::Reference; }
bool VmValue::is_null() const { return kind_ == VmValueKind::Null; }

std::int32_t VmValue::as_integer() const {
    if (!is_integer())
        throw std::runtime_error("VM value is not an integer");
    return integer_;
}

ObjectId VmValue::as_reference() const {
    if (!is_reference())
        throw std::runtime_error("VM value is not a non-null reference");
    return reference_;
}

VmFrame::VmFrame(std::size_t max_locals, std::size_t max_stack)
    : locals_(max_locals), max_stack_(max_stack) {
    if (max_stack == 0)
        throw std::runtime_error("VM frame max_stack must be greater than zero");
    stack_.reserve(max_stack);
}

std::size_t VmFrame::local_count() const { return locals_.size(); }
std::size_t VmFrame::stack_size() const { return stack_.size(); }
std::size_t VmFrame::stack_limit() const { return max_stack_; }

void VmFrame::set_local(std::size_t index, const VmValue& value) {
    if (index >= locals_.size())
        throw std::runtime_error("VM local variable index out of bounds");
    if (value.kind() == VmValueKind::Uninitialized)
        throw std::runtime_error("Cannot store an uninitialized VM value");
    locals_[index] = value;
}

const VmValue& VmFrame::get_local(std::size_t index) const {
    if (index >= locals_.size())
        throw std::runtime_error("VM local variable index out of bounds");
    if (locals_[index].kind() == VmValueKind::Uninitialized)
        throw std::runtime_error("Read of uninitialized VM local variable");
    return locals_[index];
}

void VmFrame::push(const VmValue& value) {
    if (value.kind() == VmValueKind::Uninitialized)
        throw std::runtime_error("Cannot push an uninitialized VM value");
    if (stack_.size() >= max_stack_)
        throw std::runtime_error("VM operand stack overflow");
    stack_.push_back(value);
}

VmValue VmFrame::pop() {
    if (stack_.empty())
        throw std::runtime_error("VM operand stack underflow");
    VmValue value = stack_.back();
    stack_.pop_back();
    return value;
}

const VmValue& VmFrame::peek() const {
    if (stack_.empty())
        throw std::runtime_error("VM operand stack underflow");
    return stack_.back();
}

void VmFrame::duplicate_top() {
    if (stack_.empty())
        throw std::runtime_error("VM operand stack underflow at dup");
    if (stack_.size() >= max_stack_)
        throw std::runtime_error("VM operand stack overflow at dup");
    stack_.push_back(stack_.back());
}

void VmFrame::clear_stack() { stack_.clear(); }

} // namespace solarcrystal
