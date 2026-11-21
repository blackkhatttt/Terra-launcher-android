#pragma once

#include <cstddef>
#include <string>
#include <vector>

namespace solarcrystal {

enum class CoreLibraryGroup {
    LANGUAGE,
    NUMERIC,
    COLLECTIONS,
    IO,
    NETWORK,
    THREADING,
    GRAPHICS,
    UNSUPPORTED
};

struct CoreLibraryEntry {
    const char* class_name;
    CoreLibraryGroup group;
    bool host_bridge;
    bool infdev_relevant;
};

const CoreLibraryEntry* find_core_library(const std::string& internal_name);
bool is_core_library_known(const std::string& internal_name);
bool has_core_host_bridge(const std::string& internal_name);
std::vector<CoreLibraryEntry> list_core_libraries();
std::size_t core_library_count();

} // namespace solarcrystal
