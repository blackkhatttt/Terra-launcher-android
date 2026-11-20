#pragma once
#include "class_file_model.h"
#include "class_file_repository.h"
#include "class_method_resolver.h"
#include <cstdint>
#include <string>
namespace solarcrystal {
struct ResolvedClassMember {
 std::string owner;
 std::string declaring_class;
 std::string name;
 std::string descriptor;
 bool is_field=false;
 bool from_interface=false;
};
class ClassFileLinkageResolver {
public:
 explicit ClassFileLinkageResolver(const ClassFileRepository& repository);
 ResolvedClassMember resolve(const ClassFileModel& referencing_class, std::uint16_t cp_index) const;
private:
 const ClassFileRepository& repository_;
 ClassMethodResolver methods_;
 const ClassFieldInfo* find_field(const std::string& owner,const std::string& name,const std::string& descriptor,std::string& declaring) const;
};
} // namespace solarcrystal
