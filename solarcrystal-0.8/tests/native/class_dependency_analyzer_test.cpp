#include "class_dependency_analyzer.h"

#include <cassert>
#include <cstdint>
#include <fstream>
#include <iostream>
#include <iterator>
#include <stdexcept>
#include <string>
#include <vector>

using namespace solarcrystal;

static ClassFileModel load_model(const char* path) {
    std::ifstream input(path, std::ios::binary);
    if (!input.good()) {
        throw std::runtime_error(std::string("Cannot open class file: ") + path);
    }
    const std::vector<std::uint8_t> bytes(
        (std::istreambuf_iterator<char>(input)),
        std::istreambuf_iterator<char>());
    if (bytes.empty()) {
        throw std::runtime_error("Class file is empty");
    }
    return ClassFileModel::parse(bytes.data(), bytes.size());
}

static bool contains(const std::vector<std::string>& values,
                     const std::string& expected) {
    for (const auto& value : values) {
        if (value == expected) return true;
    }
    return false;
}

int main(int argc, char** argv) {
    assert(argc == 4);
    const auto parent = load_model(argv[1]);
    const auto marker = load_model(argv[2]);
    const auto target = load_model(argv[3]);

    ClassFileRepository repository;
    repository.add(parent);
    repository.add(marker);
    repository.add(target);

    ClassDependencyAnalyzer analyzer;
    const auto report = analyzer.analyze(target, repository);

    assert(contains(report.registered, "SolarCrystalObjectFixture"));
    assert(contains(report.registered, "SolarCrystalMarkerFixture"));
    assert(contains(report.platform, "java/lang/Object"));
    assert(contains(report.unresolved, "SolarCrystalMissingFixture"));
    assert(!contains(report.registered, target.name()));
    assert(!contains(report.platform, target.name()));
    assert(!contains(report.unresolved, target.name()));

    bool unregistered_target_rejected = false;
    ClassFileRepository empty_repository;
    try {
        (void)analyzer.analyze(target, empty_repository);
    } catch (const std::runtime_error&) {
        unregistered_target_rejected = true;
    }
    assert(unregistered_target_rejected);

    std::cout << "SolarCrystal 0.8.7a class dependency analysis: PASS\n";
    std::cout << "registered, platform and unresolved dependencies: PASS\n";
}
