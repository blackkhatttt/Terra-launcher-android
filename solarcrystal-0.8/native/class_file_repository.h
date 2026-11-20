#pragma once
#include "class_file_model.h"
#include <cstddef>
#include <string>
#include <unordered_map>
#include <vector>
namespace solarcrystal {
class ClassFileRepository {
public:
 void add(const ClassFileModel& model);
 const ClassFileModel* find(const std::string& internal_name) const;
 std::vector<const ClassFileModel*> all() const;
 std::size_t size() const;
private:
 std::unordered_map<std::string, const ClassFileModel*> classes_;
};
} // namespace solarcrystal
