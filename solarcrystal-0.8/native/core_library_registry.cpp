#include "core_library_registry.h"

#include <algorithm>
#include <array>

namespace solarcrystal {
namespace {
constexpr std::array<CoreLibraryEntry, 29> kLibraries{{
    {"java/lang/Object", CoreLibraryGroup::LANGUAGE, true, true},
    {"java/lang/String", CoreLibraryGroup::LANGUAGE, true, true},
    {"java/lang/StringBuilder", CoreLibraryGroup::LANGUAGE, true, true},
    {"java/lang/StringBuffer", CoreLibraryGroup::LANGUAGE, true, false},
    {"java/lang/Class", CoreLibraryGroup::LANGUAGE, false, true},
    {"java/lang/Throwable", CoreLibraryGroup::LANGUAGE, false, true},
    {"java/lang/Exception", CoreLibraryGroup::LANGUAGE, false, true},
    {"java/lang/RuntimeException", CoreLibraryGroup::LANGUAGE, false, true},
    {"java/lang/System", CoreLibraryGroup::LANGUAGE, true, true},
    {"java/lang/Math", CoreLibraryGroup::NUMERIC, true, true},
    {"java/lang/StrictMath", CoreLibraryGroup::NUMERIC, true, false},
    {"java/lang/Integer", CoreLibraryGroup::NUMERIC, true, true},
    {"java/lang/Long", CoreLibraryGroup::NUMERIC, true, true},
    {"java/lang/Float", CoreLibraryGroup::NUMERIC, true, true},
    {"java/lang/Double", CoreLibraryGroup::NUMERIC, true, true},
    {"java/lang/Boolean", CoreLibraryGroup::NUMERIC, true, true},
    {"java/lang/Character", CoreLibraryGroup::NUMERIC, true, false},
    {"java/util/Objects", CoreLibraryGroup::COLLECTIONS, true, true},
    {"java/util/Arrays", CoreLibraryGroup::COLLECTIONS, true, true},
    {"java/util/ArrayList", CoreLibraryGroup::COLLECTIONS, false, true},
    {"java/util/HashMap", CoreLibraryGroup::COLLECTIONS, false, true},
    {"java/util/Random", CoreLibraryGroup::NUMERIC, true, true},
    {"java/io/InputStream", CoreLibraryGroup::IO, false, true},
    {"java/io/OutputStream", CoreLibraryGroup::IO, false, true},
    {"java/io/File", CoreLibraryGroup::IO, false, true},
    {"java/net/URL", CoreLibraryGroup::NETWORK, false, true},
    {"java/lang/Runnable", CoreLibraryGroup::THREADING, false, true},
    {"java/lang/Thread", CoreLibraryGroup::THREADING, false, true},
    {"java/nio/ByteBuffer", CoreLibraryGroup::IO, false, false}
}};
}

const CoreLibraryEntry* find_core_library(const std::string& internal_name) {
    const auto it = std::find_if(kLibraries.begin(), kLibraries.end(),
        [&internal_name](const CoreLibraryEntry& entry) {
            return internal_name == entry.class_name;
        });
    return it == kLibraries.end() ? nullptr : &(*it);
}

bool is_core_library_known(const std::string& internal_name) {
    return find_core_library(internal_name) != nullptr;
}

bool has_core_host_bridge(const std::string& internal_name) {
    const CoreLibraryEntry* entry = find_core_library(internal_name);
    return entry != nullptr && entry->host_bridge;
}

std::vector<CoreLibraryEntry> list_core_libraries() {
    return std::vector<CoreLibraryEntry>(kLibraries.begin(), kLibraries.end());
}

std::size_t core_library_count() {
    return kLibraries.size();
}

} // namespace solarcrystal
