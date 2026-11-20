#include "class_method_resolver.h"
#include <set>
#include <stdexcept>
#include <vector>
namespace solarcrystal {
namespace {
constexpr std::uint16_t kAccStatic=0x0008;
constexpr std::uint16_t kAccPrivate=0x0002;
constexpr std::uint16_t kAccAbstract=0x0400;
bool eligible_instance_method(const ClassMethodInfo& method) {
 return (method.access_flags & (kAccStatic|kAccPrivate))==0;
}
bool concrete_instance_method(const ClassMethodInfo& method) {
 return eligible_instance_method(method) &&
        (method.access_flags & kAccAbstract)==0 && method.has_code;
}
}
ClassMethodResolver::ClassMethodResolver(const ClassFileRepository& repository)
 : repository_(repository) {}
ResolvedClassMethod ClassMethodResolver::search_interface(
 const std::string& name,const std::string& method_name,
 const std::string& descriptor,std::set<std::string>& visited) const {
 if(!visited.insert(name).second) return {};
 const auto* type=repository_.find(name);
 if(!type) throw std::runtime_error("Interface dependency is not registered: "+name);
 constexpr std::uint16_t kAccInterface=0x0200;
 if((type->access_flags()&kAccInterface)==0)
  throw std::runtime_error("Expected interface metadata: "+name);
 if(const auto* method=type->find_method(method_name,descriptor)) {
  if(eligible_instance_method(*method))
   return {type,method,true};
 }
 for(const auto& parent:type->interfaces()) {
  auto result=search_interface(parent,method_name,descriptor,visited);
  if(result.method) return result;
 }
 return {};
}
ResolvedClassMethod ClassMethodResolver::resolve_interface_method(
 const std::string& interface_name,const std::string& name,
 const std::string& descriptor) const {
 std::set<std::string> visited;
 return search_interface(interface_name,name,descriptor,visited);
}
ResolvedClassMethod ClassMethodResolver::resolve_virtual_method(
 const std::string& receiver_class,const std::string& name,
 const std::string& descriptor) const {
 std::set<std::string> visited_classes;
 std::vector<std::string> interfaces;
 std::string current=receiver_class;
 while(!current.empty() && current!="java/lang/Object") {
  if(!visited_classes.insert(current).second)
   throw std::runtime_error("Circular superclass chain detected during method resolution");
  const auto* type=repository_.find(current);
  if(!type) throw std::runtime_error("Class dependency is not registered: "+current);
  for(const auto& interface_name:type->interfaces()) interfaces.push_back(interface_name);
  if(const auto* method=type->find_method(name,descriptor)) {
   if(eligible_instance_method(*method)) return {type,method,false};
  }
  current=type->super_name();
 }
 std::set<std::string> visited_interfaces;
 ResolvedClassMethod selected;
 for(const auto& interface_name:interfaces) {
  auto candidate=search_interface(interface_name,name,descriptor,visited_interfaces);
  if(!candidate.method || !concrete_instance_method(*candidate.method)) continue;
  if(selected.method && selected.declaring_class!=candidate.declaring_class)
   throw std::runtime_error("Ambiguous interface default method resolution");
  selected=candidate;
 }
 return selected;
}
} // namespace solarcrystal
