#include "class_file_execution_resolver.h"
#include <set>
#include <stdexcept>
namespace solarcrystal {
ClassFileExecutionResolver::ClassFileExecutionResolver(const ClassFileModel& model)
 : model_(model), repository_(nullptr) {}
ClassFileExecutionResolver::ClassFileExecutionResolver(
 const ClassFileModel& model, const ClassFileRepository& repository)
 : model_(model), repository_(&repository) {
 if (repository_->find(model.name()) != &model)
  throw std::runtime_error("Resolver class is not registered in repository");
}
const ClassFileModel* ClassFileExecutionResolver::find_class(const std::string& name) const {
 if (repository_) return repository_->find(name);
 return name == model_.name() ? &model_ : nullptr;
}
const ClassFieldInfo* ClassFileExecutionResolver::find_inherited_field(
 const std::string& class_name, const std::string& field_name,
 const std::string& descriptor) const {
 std::set<std::string> visited;
 std::string current = class_name;
 while (!current.empty()) {
  if (!visited.insert(current).second)
   throw std::runtime_error("Circular superclass chain detected");
  const auto* type = find_class(current);
  if (!type) throw std::runtime_error("Superclass is not registered: " + current);
  for (const auto& field : type->fields())
   if (field.name == field_name && field.descriptor == descriptor) return &field;
  current = type->super_name();
  if (current == "java/lang/Object") break;
 }
 return nullptr;
}
bool ClassFileExecutionResolver::is_same_or_subclass(
 const std::string& candidate, const std::string& base) const {
 std::set<std::string> visited;
 std::string current = candidate;
 while (!current.empty()) {
  if (current == base) return true;
  if (!visited.insert(current).second)
   throw std::runtime_error("Circular superclass chain detected");
  const auto* type = find_class(current);
  if (!type) return false;
  current = type->super_name();
  if (current == "java/lang/Object") break;
 }
 return false;
}
NewClassResolver ClassFileExecutionResolver::new_class_resolver() const {
 return [this](std::uint16_t index) {
  const auto name = model_.class_name_at(index);
  if (name.empty()) throw std::runtime_error("CONSTANT_Class resolved to an empty name");
  if (!find_class(name)) throw std::runtime_error("Class is not registered: " + name);
  return name;
 };
}
SpecialMethodResolver ClassFileExecutionResolver::special_method_resolver() const {
 return [this](std::uint16_t index) {
  const auto ref = model_.member_reference_at(index);
  if (ref.tag != 10 && ref.tag != 11)
   throw std::runtime_error("invokespecial requires Methodref or InterfaceMethodref");
  if (ref.name != "<init>" || ref.descriptor != "()V")
   throw std::runtime_error("Only no-argument void constructors are supported");
  if (ref.owner == "java/lang/Object")
   return SpecialMethod{ref.owner, ref.name, ref.descriptor, {}, 1, 1};
  const auto* owner = find_class(ref.owner);
  if (!owner) throw std::runtime_error("Constructor owner is not registered: " + ref.owner);
  const auto* method = owner->find_method(ref.name, ref.descriptor);
  if (!method || !method->has_code)
   throw std::runtime_error("Constructor method has no executable Code attribute");
  return SpecialMethod{owner->name(), method->name, method->descriptor,
      method->code, method->max_locals, method->max_stack};
 };
}
InstanceFieldResolver ClassFileExecutionResolver::instance_field_resolver() const {
 return [this](std::uint16_t index) {
  const auto ref = model_.member_reference_at(index);
  if (ref.tag != 9) throw std::runtime_error("getfield/putfield requires Fieldref");
  if (ref.descriptor != "I") throw std::runtime_error("Only integer instance fields are supported");
  if (!find_class(ref.owner))
   throw std::runtime_error("Field access class is not registered: " + ref.owner);
  const auto* declaration = find_inherited_field(ref.owner, ref.name, ref.descriptor);
  if (!declaration)
   throw std::runtime_error("Field reference does not match a declared field in its superclass chain");
  std::vector<std::string> compatible;
  if (repository_) {
   for (const auto* candidate : repository_->all())
    if (is_same_or_subclass(candidate->name(), ref.owner))
     compatible.push_back(candidate->name());
  } else {
   compatible.push_back(model_.name());
  }
  return InstanceField{declaration->name, declaration->descriptor, ref.owner,
      declaration->name, compatible};
 };
}
} // namespace solarcrystal
