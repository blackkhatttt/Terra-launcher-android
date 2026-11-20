#!/usr/bin/env python3
from pathlib import Path
root = Path.cwd()
base = Path(__file__).resolve().parent
cpp = root / "terra-android/src/main/cpp"
native = base.parent / "native"
include_dir = cpp / "include"
if not include_dir.is_dir():
    raise SystemExit("Native include directory missing")
(include_dir / "class_file_model.h").write_text(
    (native / "class_file_model.h").read_text(encoding="utf-8"), encoding="utf-8")
(cpp / "class_file_model.cpp").write_text(
    (native / "class_file_model.cpp").read_text(encoding="utf-8"), encoding="utf-8")
cmake = cpp / "CMakeLists.txt"
source = cmake.read_text(encoding="utf-8")
if "class_file_model.cpp" not in source:
    marker = "    reference_bytecode_executor.cpp"
    if marker not in source:
        raise SystemExit("Could not locate reference_bytecode_executor.cpp in CMake source list")
    cmake.write_text(source.replace(marker, marker + "\n    class_file_model.cpp", 1), encoding="utf-8")
if "ClassFileModel::parse" not in (cpp / "class_file_model.cpp").read_text(encoding="utf-8"):
    raise SystemExit("Class-file model integration validation failed")
print("SolarCrystal 0.8.6a class-file model integrated.")
