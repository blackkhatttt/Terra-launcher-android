#include "class_file_repository.h"

#include <stdexcept>

namespace solarcrystal {

void ClassFileRepository::add(const ClassFileModel& model) {
    if (model.name().empty()) {
        throw std::runtime_error("Cannot register a class with an empty name");
    }
    const auto inserted = classes_.emplace(model.name(), &model);
    if (!inserted.second) {
        throw std::runtime_error("Duplicate class registration: " + model.name());
    }
}

const ClassFileModel* ClassFileRepository::find(
    const std::string& internal_name) const {
    const auto found = classes_.find(internal_name);
    return found == classes_.end() ? nullptr : found->second;
}

std::size_t ClassFileRepository::size() const {
    return classes_.size();
}

} // namespace solarcrystal
