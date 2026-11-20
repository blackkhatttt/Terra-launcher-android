#pragma once

#include "class_file_model.h"
#include "class_file_repository.h"

#include <string>
#include <vector>

namespace solarcrystal {

struct ClassDependencyReport {
    std::vector<std::string> registered;
    std::vector<std::string> platform;
    std::vector<std::string> unresolved;
};

class ClassDependencyAnalyzer {
public:
    ClassDependencyReport analyze(
        const ClassFileModel& model,
        const ClassFileRepository& repository) const;
};

} // namespace solarcrystal
