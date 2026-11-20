#pragma once

#include "class_file_model.h"

#include <cstddef>
#include <string>
#include <unordered_map>

namespace solarcrystal {

// Non-owning registry of parsed classes. ClassFileModel instances must outlive
// the repository and every resolver created from it.
class ClassFileRepository {
public:
    void add(const ClassFileModel& model);
    const ClassFileModel* find(const std::string& internal_name) const;
    std::size_t size() const;

private:
    std::unordered_map<std::string, const ClassFileModel*> classes_;
};

} // namespace solarcrystal
