#pragma once

#include "object_heap.h"

#include <cstddef>
#include <cstdint>
#include <vector>

namespace solarcrystal {

enum class VmValueKind : std::uint8_t {
    Uninitialized = 0,
    Integer = 1,
    Reference = 2,
    Null = 3
};

class VmValue {
public:
    VmValue() = default;

    static VmValue integer(std::int32_t value);
    static VmValue reference(ObjectId object_id);
    static VmValue null_reference();

    VmValueKind kind() const;
    bool is_integer() const;
    bool is_reference() const;
    bool is_null() const;
    std::int32_t as_integer() const;
    ObjectId as_reference() const;

private:
    VmValueKind kind_ = VmValueKind::Uninitialized;
    std::int32_t integer_ = 0;
    ObjectId reference_ = 0;
};

class VmFrame {
public:
    VmFrame(std::size_t max_locals, std::size_t max_stack);

    std::size_t local_count() const;
    std::size_t stack_size() const;
    std::size_t stack_limit() const;

    void set_local(std::size_t index, const VmValue& value);
    const VmValue& get_local(std::size_t index) const;

    void push(const VmValue& value);
    VmValue pop();
    const VmValue& peek() const;
    void duplicate_top();
    void clear_stack();

private:
    std::vector<VmValue> locals_;
    std::vector<VmValue> stack_;
    std::size_t max_stack_;
};

} // namespace solarcrystal
