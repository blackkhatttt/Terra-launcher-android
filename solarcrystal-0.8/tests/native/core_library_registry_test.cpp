#include "core_library_registry.h"

#include <cassert>
#include <iostream>

int main() {
    using namespace solarcrystal;

    assert(core_library_count() == 29);
    assert(is_core_library_known("java/lang/Object"));
    assert(is_core_library_known("java/lang/String"));
    assert(is_core_library_known("java/util/ArrayList"));
    assert(is_core_library_known("java/io/InputStream"));
    assert(is_core_library_known("java/net/URL"));
    assert(!is_core_library_known("net/minecraft/client/Minecraft"));

    assert(has_core_host_bridge("java/lang/Math"));
    assert(has_core_host_bridge("java/lang/System"));
    assert(has_core_host_bridge("java/lang/Integer"));
    assert(!has_core_host_bridge("java/util/ArrayList"));
    assert(!has_core_host_bridge("java/lang/Thread"));

    const CoreLibraryEntry* math = find_core_library("java/lang/Math");
    assert(math != nullptr && math->infdev_relevant);
    assert(find_core_library("missing/Type") == nullptr);

    const auto entries = list_core_libraries();
    assert(entries.size() == core_library_count());

    std::cout << "SolarCrystal core Java library registry: "
              << entries.size() << " entries passed.\n";
    return 0;
}
