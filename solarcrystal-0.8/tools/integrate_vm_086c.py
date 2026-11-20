#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
base = Path(__file__).resolve().parent
native = base.parent / "native"
cpp = root / "terra-android/src/main/cpp"
include_dir = cpp / "include"

if not include_dir.is_dir():
    raise SystemExit("Native include directory missing")

for name in ("class_file_repository.h", "class_file_execution_resolver.h"):
    (include_dir / name).write_text(
        (native / name).read_text(encoding="utf-8"), encoding="utf-8"
    )

for name in ("class_file_repository.cpp", "class_file_execution_resolver.cpp"):
    (cpp / name).write_text(
        (native / name).read_text(encoding="utf-8"), encoding="utf-8"
    )

cmake = cpp / "CMakeLists.txt"
source = cmake.read_text(encoding="utf-8")
if "class_file_repository.cpp" not in source:
    marker = "    class_file_model.cpp"
    if marker not in source:
        raise SystemExit("Could not locate class_file_model.cpp in CMake source list")
    source = source.replace(
        marker, marker + "\n    class_file_repository.cpp", 1
    )
cmake.write_text(source, encoding="utf-8")

repository = (cpp / "class_file_repository.cpp").read_text(encoding="utf-8")
resolver = (cpp / "class_file_execution_resolver.cpp").read_text(encoding="utf-8")
if "ClassFileRepository::add" not in repository:
    raise SystemExit("Class repository integration validation failed")
if "repository_->find" not in resolver:
    raise SystemExit("Multi-class resolver integration validation failed")

print("SolarCrystal 0.8.6c multi-class repository integrated.")
