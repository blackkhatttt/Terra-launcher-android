#pragma once
#include "class_file_model.h"
#include "class_file_repository.h"
#include "reference_bytecode_executor.h"
namespace solarcrystal {
class ClassFileExecutionResolver {
public:
 explicit ClassFileExecutionResolver(const ClassFileModel& model);
 ClassFileExecutionResolver(const ClassFileModel& model, const ClassFileRepository& repository);
 NewClassResolver new_class_resolver() const;
 SpecialMethodResolver special_method_resolver() const;
 InstanceFieldResolver instance_field_resolver() const;
private:
 const ClassFileModel& model_;
 const ClassFileRepository* repository_;
 const ClassFileModel* find_class(const std::string& internal_name) const;
 const ClassFieldInfo* find_inherited_field(const std::string& class_name,
     const std::string& field_name, const std::string& descriptor) const;
 bool is_same_or_subclass(const std::string& candidate, const std::string& base) const;
};
} // namespace solarcrystal
