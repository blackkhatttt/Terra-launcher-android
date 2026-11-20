#include "class_file_execution_resolver.h"

#include <stdexcept>

namespace solarcrystal {

ClassFileExecutionResolver::ClassFileExecutionResolver(const ClassFileModel& model)
    : model_(model), repository_(nullptr) {}

ClassFileExecutionResolver::ClassFileExecutionResolver(
    const ClassFileModel& model, const ClassFileRepository& repository)
    : model_(model), repository_(&repository) {
    if (repository_->find(model.name()) != &model) {
        throw std::runtime_error("Resolver class is not registered in repository");
    }
}

const ClassFileModel* ClassFileExecutionResolver::find_class(
    const std::string& internal_name) const {
    if (repository_) {
        return repository_->find(internal_name);
    }
    return internal_name == model_.name() ? &model_ : nullptr;
}

NewClassResolver ClassFileExecutionResolver::new_class_resolver() const {
    return [this](std::uint16_t index) {
        const auto name = model_.class_name_at(index);
        if (name.empty()) {
            throw std::runtime_error("CONSTANT_Class resolved to an empty name");
        }
        if (!find_class(name)) {
            throw std::runtime_error("Class is not registered: " + name);
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

        const auto* owner = find_class(reference.owner);
        if (!owner) {
            throw std::runtime_error("Constructor owner is not registered: " + reference.owner);
        }
        const auto* method = owner->find_method(reference.name, reference.descriptor);
        if (!method || !method->has_code) {
            throw std::runtime_error("Constructor method has no executable Code attribute");
        }
        return SpecialMethod{
            owner->name(), method->name, method->descriptor, method->code,
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
        if (reference.descriptor != "I") {
            throw std::runtime_error("Only integer instance fields are supported");
        }
        const auto* owner = find_class(reference.owner);
        if (!owner) {
            throw std::runtime_error("Field owner is not registered: " + reference.owner);
        }

        bool found = false;
        for (const auto& field : owner->fields()) {
            if (field.name == reference.name && field.descriptor == reference.descriptor) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw std::runtime_error("Field reference does not match a declared field");
        }
        return InstanceField{owner->name(), reference.name};
    };
}

} // namespace solarcrystal
