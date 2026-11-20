#include "class_file_execution_resolver.h"

#include <stdexcept>
#include <utility>

namespace solarcrystal {

ClassFileExecutionResolver::ClassFileExecutionResolver(const ClassFileModel& model)
    : model_(model) {}

NewClassResolver ClassFileExecutionResolver::new_class_resolver() const {
    return [this](std::uint16_t index) {
        const auto name = model_.class_name_at(index);
        if (name.empty()) {
            throw std::runtime_error("CONSTANT_Class resolved to an empty name");
        }
        return name;
    };
}

SpecialMethodResolver ClassFileExecutionResolver::special_method_resolver() const {
    return [this](std::uint16_t index) {
        const auto reference = model_.member_reference_at(index);
        if (reference.tag != 10 && reference.tag != 11) {
            throw std::runtime_error("invokespecial requires Methodref or InterfaceMethodref");
        }
        if (reference.name != "<init>" || reference.descriptor != "()V") {
            throw std::runtime_error("Only no-argument void constructors are supported");
        }

        if (reference.owner == "java/lang/Object") {
            return SpecialMethod{
                reference.owner, reference.name, reference.descriptor, {}, 1, 1
            };
        }

        if (reference.owner != model_.name()) {
            throw std::runtime_error("Constructor owner is outside the loaded class model");
        }

        const auto* method = model_.find_method(reference.name, reference.descriptor);
        if (!method || !method->has_code) {
            throw std::runtime_error("Constructor method has no executable Code attribute");
        }

        return SpecialMethod{
            model_.name(), method->name, method->descriptor, method->code,
            method->max_locals, method->max_stack
        };
    };
}

InstanceFieldResolver ClassFileExecutionResolver::instance_field_resolver() const {
    return [this](std::uint16_t index) {
        const auto reference = model_.member_reference_at(index);
        if (reference.tag != 9) {
            throw std::runtime_error("getfield/putfield requires Fieldref");
        }
        if (reference.owner != model_.name()) {
            throw std::runtime_error("Instance field owner is outside the loaded class model");
        }
        if (reference.descriptor != "I") {
            throw std::runtime_error("Only integer instance fields are supported");
        }

        bool found = false;
        for (const auto& field : model_.fields()) {
            if (field.name == reference.name && field.descriptor == reference.descriptor) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw std::runtime_error("Field reference does not match a declared field");
        }

        return InstanceField{reference.owner, reference.name};
    };
}

} // namespace solarcrystal
