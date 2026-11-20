#include "class_dependency_analyzer.h"

#include <algorithm>
#include <set>
#include <stdexcept>

namespace solarcrystal {
namespace {

bool is_platform_class(const std::string& name) {
    return name.rfind("java/", 0) == 0 ||
           name.rfind("javax/", 0) == 0 ||
           name.rfind("jdk/", 0) == 0 ||
           name.rfind("sun/", 0) == 0 ||
           name.rfind("android/", 0) == 0;
}

void add_descriptor_dependencies(const std::string& descriptor,
                                 std::set<std::string>& dependencies) {
    for (std::size_t i = 0; i < descriptor.size(); ++i) {
        if (descriptor[i] != 'L') {
            continue;
        }
        const auto end = descriptor.find(';', i + 1);
        if (end == std::string::npos || end == i + 1) {
            throw std::runtime_error("Malformed reference type in class descriptor");
        }
        const auto name = descriptor.substr(i + 1, end - i - 1);
        if (name.front() == '[' || name.find('.') != std::string::npos ||
            name.find(';') != std::string::npos) {
            throw std::runtime_error("Invalid internal class name in descriptor");
        }
        dependencies.insert(name);
        i = end;
    }
}

} // namespace

ClassDependencyReport ClassDependencyAnalyzer::analyze(
    const ClassFileModel& model,
    const ClassFileRepository& repository) const {
    if (repository.find(model.name()) != &model) {
        throw std::runtime_error("Dependency analysis class is not registered in repository");
    }

    std::set<std::string> dependencies;
    if (!model.super_name().empty()) {
        dependencies.insert(model.super_name());
    }
    for (const auto& interface_name : model.interfaces()) {
        dependencies.insert(interface_name);
    }
    for (const auto& field : model.fields()) {
        add_descriptor_dependencies(field.descriptor, dependencies);
    }
    for (const auto& method : model.methods()) {
        add_descriptor_dependencies(method.descriptor, dependencies);
    }

    const auto& pool = model.constant_pool();
    for (std::size_t i = 1; i < pool.size(); ++i) {
        if (pool[i].tag != 7) {
            continue;
        }
        const auto index = static_cast<std::uint16_t>(i);
        auto name = model.class_name_at(index);
        if (!name.empty() && name.front() == '[') {
            add_descriptor_dependencies(name, dependencies);
        } else if (!name.empty()) {
            dependencies.insert(std::move(name));
        }
    }

    ClassDependencyReport report;
    for (const auto& name : dependencies) {
        if (name == model.name()) {
            continue;
        }
        if (repository.find(name)) {
            report.registered.push_back(name);
        } else if (is_platform_class(name)) {
            report.platform.push_back(name);
        } else {
            report.unresolved.push_back(name);
        }
    }
    return report;
}

} // namespace solarcrystal
