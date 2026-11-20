#pragma once

#include "class_file_model.h"
#include "reference_bytecode_executor.h"

namespace solarcrystal {

// Adapts one parsed class file's Constant Pool to the bounded reference executor.
// This is deliberately a single-class resolver: it does not perform classpath lookup.
class ClassFileExecutionResolver {
public:
    explicit ClassFileExecutionResolver(const ClassFileModel& model);

    NewClassResolver new_class_resolver() const;
    SpecialMethodResolver special_method_resolver() const;
    InstanceFieldResolver instance_field_resolver() const;

private:
    const ClassFileModel& model_;
};

} // namespace solarcrystal
