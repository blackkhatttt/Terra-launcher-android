#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
base = Path(__file__).resolve().parent
native = base.parent / "native"
cpp = root / "terra-android/src/main/cpp"
include_dir = cpp / "include"

if not include_dir.is_dir():
    raise SystemExit("Native include directory missing")

for name in ("class_dependency_analyzer.h",):
    (include_dir / name).write_text(
        (native / name).read_text(encoding="utf-8"), encoding="utf-8"
    )

(cpp / "class_dependency_analyzer.cpp").write_text(
    (native / "class_dependency_analyzer.cpp").read_text(encoding="utf-8"),
    encoding="utf-8",
)

cmake = cpp / "CMakeLists.txt"
source = cmake.read_text(encoding="utf-8")
if "class_dependency_analyzer.cpp" not in source:
    marker = "    class_file_method_executor.cpp"
    if marker not in source:
        raise SystemExit("Could not locate class_file_method_executor.cpp in CMake source list")
    source = source.replace(
        marker, marker + "\n    class_dependency_analyzer.cpp", 1
    )
cmake.write_text(source, encoding="utf-8")

implementation = (cpp / "class_dependency_analyzer.cpp").read_text(encoding="utf-8")
if "ClassDependencyAnalyzer::analyze" not in implementation:
    raise SystemExit("Dependency analyzer integration validation failed")

print("SolarCrystal 0.8.7a class dependency analyzer integrated.")
