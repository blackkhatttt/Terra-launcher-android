#include "class_file_repository.h"
#include <stdexcept>
namespace solarcrystal {
void ClassFileRepository::add(const ClassFileModel& model) {
 if(model.name().empty()) throw std::runtime_error("Cannot register a class with an empty name");
 const auto inserted=classes_.emplace(model.name(),&model);
 if(!inserted.second) throw std::runtime_error("Duplicate class registration: "+model.name());
}
const ClassFileModel* ClassFileRepository::find(const std::string& name) const {
 const auto found=classes_.find(name);
 return found==classes_.end()?nullptr:found->second;
}
std::vector<const ClassFileModel*> ClassFileRepository::all() const {
 std::vector<const ClassFileModel*> result;
 result.reserve(classes_.size());
 for(const auto& entry:classes_) result.push_back(entry.second);
 return result;
}
std::size_t ClassFileRepository::size() const { return classes_.size(); }
} // namespace solarcrystal
