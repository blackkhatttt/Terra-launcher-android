#include "class_file_linkage_resolver.h"
#include <set>
#include <stdexcept>
namespace solarcrystal {
ClassFileLinkageResolver::ClassFileLinkageResolver(const ClassFileRepository& repository)
 : repository_(repository), methods_(repository) {}
const ClassFieldInfo* ClassFileLinkageResolver::find_field(
 const std::string& owner,const std::string& name,const std::string& descriptor,
 std::string& declaring) const {
 std::set<std::string> visited;
 std::string current=owner;
 while(!current.empty() && current!="java/lang/Object") {
  if(!visited.insert(current).second) throw std::runtime_error("Circular superclass chain during field linkage");
  const auto* type=repository_.find(current);
  if(!type) throw std::runtime_error("Field owner class is not registered: "+current);
  for(const auto& field:type->fields()) {
   if(field.name==name && field.descriptor==descriptor) { declaring=current; return &field; }
  }
  current=type->super_name();
 }
 return nullptr;
}
ResolvedClassMember ClassFileLinkageResolver::resolve(
 const ClassFileModel& referencing_class,std::uint16_t cp_index) const {
 const auto ref=referencing_class.member_reference_at(cp_index);
 const auto* owner=repository_.find(ref.owner);
 if(!owner) throw std::runtime_error("Member owner is not registered: "+ref.owner);
 ResolvedClassMember result;
 result.owner=ref.owner; result.name=ref.name; result.descriptor=ref.descriptor;
 if(ref.tag==9) {
  result.is_field=true;
  const auto* field=find_field(ref.owner,ref.name,ref.descriptor,result.declaring_class);
  if(!field) throw std::runtime_error("Unresolved field reference: "+ref.owner+"."+ref.name+ref.descriptor);
  return result;
 }
 if(ref.tag!=10 && ref.tag!=11) throw std::runtime_error("Unsupported constant-pool member reference tag");
 if(ref.name=="<init>") {
  const auto* constructor=owner->find_method(ref.name,ref.descriptor);
  if(!constructor) throw std::runtime_error("Unresolved constructor reference: "+ref.owner+ref.descriptor);
  result.declaring_class=ref.owner;
  return result;
 }
 ResolvedClassMethod method;
 if(ref.tag==11) method=methods_.resolve_interface_method(ref.owner,ref.name,ref.descriptor);
 else method=methods_.resolve_virtual_method(ref.owner,ref.name,ref.descriptor);
 if(!method.method) throw std::runtime_error("Unresolved method reference: "+ref.owner+"."+ref.name+ref.descriptor);
 result.declaring_class=method.declaring_class->name();
 result.from_interface=method.from_interface;
 return result;
}
} // namespace solarcrystal
