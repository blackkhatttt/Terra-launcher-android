#include "object_heap.h"

#include <cassert>
#include <cstdint>
#include <iostream>
#include <stdexcept>
#include <vector>

using solarcrystal::ObjectField;
using solarcrystal::ObjectHeap;

int main() {
    ObjectHeap heap;
    const auto box = heap.allocate("SolarCrystalBox", {
        ObjectField{"value", 0},
        ObjectField{"count", 2}
    });

    assert(heap.is_valid(box));
    assert(heap.size() == 1);
    assert(heap.get_int(box, "SolarCrystalBox", "value") == 0);

    heap.put_int(box, "SolarCrystalBox", "value", 37);
    assert(heap.get_int(box, "SolarCrystalBox", "value") == 37);

    bool null_rejected = false;
    try { (void)heap.get_int(0, "SolarCrystalBox", "value"); }
    catch (const std::runtime_error&) { null_rejected = true; }
    assert(null_rejected);

    bool wrong_class_rejected = false;
    try { (void)heap.get_int(box, "OtherClass", "value"); }
    catch (const std::runtime_error&) { wrong_class_rejected = true; }
    assert(wrong_class_rejected);

    bool missing_field_rejected = false;
    try { (void)heap.get_int(box, "SolarCrystalBox", "missing"); }
    catch (const std::runtime_error&) { missing_field_rejected = true; }
    assert(missing_field_rejected);

    bool duplicate_rejected = false;
    try {
        (void)heap.allocate("BadClass", {
            ObjectField{"same", 1}, ObjectField{"same", 2}
        });
    } catch (const std::runtime_error&) { duplicate_rejected = true; }
    assert(duplicate_rejected);

    bool invalid_reference_rejected = false;
    try { (void)heap.get_int(999, "SolarCrystalBox", "value"); }
    catch (const std::runtime_error&) { invalid_reference_rejected = true; }
    assert(invalid_reference_rejected);

    for (std::size_t i = 1; i < ObjectHeap::MAX_OBJECTS; ++i)
        (void)heap.allocate("HeapLimitProbe", {ObjectField{"n", 0}});
    bool heap_limit_rejected = false;
    try { (void)heap.allocate("Overflow", {}); }
    catch (const std::runtime_error&) { heap_limit_rejected = true; }
    assert(heap_limit_rejected);
    assert(heap.size() == ObjectHeap::MAX_OBJECTS);

    std::cout << "SolarCrystal ObjectHeap: PASS\n";
    std::cout << "Allocation, field read/write, class checks and invalid refs: PASS\n";
    return 0;
}
