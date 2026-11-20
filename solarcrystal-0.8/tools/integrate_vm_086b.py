#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
base = Path(__file__).resolve().parent
native = base.parent / "native"
cpp = root / "terra-android/src/main/cpp"
include_dir = cpp / "include"

if not include_dir.is_dir():
    raise SystemExit("Native include directory missing")

(include_dir / "class_file_execution_resolver.h").write_text(
    (native / "class_file_execution_resolver.h").read_text(encoding="utf-8"),
    encoding="utf-8",
)
(cpp / "class_file_execution_resolver.cpp").write_text(
    (native / "class_file_execution_resolver.cpp").read_text(encoding="utf-8"),
    encoding="utf-8",
)

cmake = cpp / "CMakeLists.txt"
source = cmake.read_text(encoding="utf-8")
if "class_file_execution_resolver.cpp" not in source:
    marker = "    class_file_model.cpp"
    if marker not in source:
        raise SystemExit("Could not locate class_file_model.cpp in CMake source list")
    cmake.write_text(
        source.replace(marker, marker + "\n    class_file_execution_resolver.cpp", 1),
        encoding="utf-8",
    )

integrated = (cpp / "class_file_execution_resolver.cpp").read_text(encoding="utf-8")
if "ClassFileExecutionResolver::special_method_resolver" not in integrated:
    raise SystemExit("Constant Pool resolver integration validation failed")

print("SolarCrystal 0.8.6b Constant Pool execution resolver integrated.")
