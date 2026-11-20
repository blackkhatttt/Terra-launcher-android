#pragma once
#include "class_file_repository.h"
#include <set>
#include <string>
namespace solarcrystal {
struct ResolvedClassMethod {
 const ClassFileModel* declaring_class = nullptr;
 const ClassMethodInfo* method = nullptr;
 bool from_interface = false;
};
class ClassMethodResolver {
public:
 explicit ClassMethodResolver(const ClassFileRepository& repository);
 ResolvedClassMethod resolve_virtual_method(const std::string& receiver_class,
     const std::string& name, const std::string& descriptor) const;
 ResolvedClassMethod resolve_interface_method(const std::string& interface_name,
     const std::string& name, const std::string& descriptor) const;
private:
 const ClassFileRepository& repository_;
 ResolvedClassMethod search_interface(const std::string& interface_name,
     const std::string& name, const std::string& descriptor,
     std::set<std::string>& visited) const;
};
} // namespace solarcrystal
