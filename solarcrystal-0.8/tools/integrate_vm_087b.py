#!/usr/bin/env python3
from pathlib import Path
root=Path.cwd()
base=Path(__file__).resolve().parent
native=base.parent/"native"
cpp=root/"terra-android/src/main/cpp"
include_dir=cpp/"include"
if not include_dir.is_dir(): raise SystemExit("Native include directory missing")
for name in ("class_file_repository.h","class_file_execution_resolver.h","reference_bytecode_executor.h"):
 (include_dir/name).write_text((native/name).read_text(encoding="utf-8"),encoding="utf-8")
for name in ("class_file_execution_resolver.cpp","reference_bytecode_executor.cpp"):
 (cpp/name).write_text((native/name).read_text(encoding="utf-8"),encoding="utf-8")
cmake=(cpp/"CMakeLists.txt").read_text(encoding="utf-8")
for name in ("class_file_execution_resolver.cpp","reference_bytecode_executor.cpp"):
 if name not in cmake: raise SystemExit("Required source missing from CMake: "+name)
if "find_inherited_field" not in (cpp/"class_file_execution_resolver.cpp").read_text(encoding="utf-8"):
 raise SystemExit("Inherited field resolver integration validation failed")
print("SolarCrystal 0.8.7b inherited field resolution integrated.")
