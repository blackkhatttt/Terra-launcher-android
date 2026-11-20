#include "vm_value.h"

#include <cassert>
#include <cstdint>
#include <iostream>
#include <stdexcept>

using solarcrystal::ObjectId;
using solarcrystal::VmFrame;
using solarcrystal::VmValue;
using solarcrystal::VmValueKind;

template <typename Function>
static bool throws_runtime_error(Function function) {
    try {
        function();
    } catch (const std::runtime_error&) {
        return true;
    }
    return false;
}

int main() {
    const VmValue number = VmValue::integer(37);
    const VmValue object = VmValue::reference(static_cast<ObjectId>(12));
    const VmValue null_value = VmValue::null_reference();

    assert(number.kind() == VmValueKind::Integer);
    assert(number.as_integer() == 37);
    assert(object.kind() == VmValueKind::Reference);
    assert(object.as_reference() == 12);
    assert(null_value.is_null());

    assert(throws_runtime_error([&] { (void)object.as_integer(); }));
    assert(throws_runtime_error([&] { (void)null_value.as_reference(); }));
    assert(throws_runtime_error([] { (void)VmValue::reference(0); }));

    VmFrame frame(3, 4);
    assert(frame.local_count() == 3);
    assert(frame.stack_limit() == 4);
    assert(throws_runtime_error([&] { (void)frame.get_local(0); }));

    frame.set_local(0, number);
    frame.set_local(1, object);
    frame.set_local(2, null_value);
    assert(frame.get_local(0).as_integer() == 37);
    assert(frame.get_local(1).as_reference() == 12);
    assert(frame.get_local(2).is_null());

    assert(throws_runtime_error([&] { frame.set_local(3, number); }));

    frame.push(number);
    frame.push(object);
    frame.duplicate_top();
    assert(frame.stack_size() == 3);
    assert(frame.pop().as_reference() == 12);
    assert(frame.pop().as_reference() == 12);
    assert(frame.pop().as_integer() == 37);
    assert(throws_runtime_error([&] { (void)frame.pop(); }));

    frame.push(number);
    frame.push(number);
    frame.push(number);
    frame.push(number);
    assert(throws_runtime_error([&] { frame.push(number); }));
    frame.clear_stack();
    assert(frame.stack_size() == 0);

    assert(throws_runtime_error([] { VmFrame invalid(1, 0); }));

    std::cout << "SolarCrystal VmValue: PASS\n";
    std::cout << "Tagged values, nulls, locals, stack bounds and dup: PASS\n";
    return 0;
}
