#!/usr/bin/env python3
from pathlib import Path
root=Path.cwd()
base=Path(__file__).resolve().parent
native=base.parent/"native"
cpp=root/"terra-android/src/main/cpp"
include_dir=cpp/"include"
if not include_dir.is_dir(): raise SystemExit("Native include directory missing")
for name in ("vm_array_heap.h","array_bytecode_executor.h"):
 (include_dir/name).write_text((native/name).read_text(encoding="utf-8"),encoding="utf-8")
for name in ("vm_array_heap.cpp","array_bytecode_executor.cpp"):
 (cpp/name).write_text((native/name).read_text(encoding="utf-8"),encoding="utf-8")
cmake_path=cpp/"CMakeLists.txt"
cmake=cmake_path.read_text(encoding="utf-8")
for source in ("vm_array_heap.cpp","array_bytecode_executor.cpp"):
 if source not in cmake:
  lines=cmake.splitlines()
  insertion=next((i+1 for i,line in enumerate(lines) if "reference_bytecode_executor.cpp" in line),None)
  if insertion is None: raise SystemExit("Could not find reference bytecode executor in CMakeLists.txt")
  lines.insert(insertion,"    "+source)
  cmake="\n".join(lines)+"\n"
cmake_path.write_text(cmake,encoding="utf-8")
for source in ("vm_array_heap.cpp","array_bytecode_executor.cpp"):
 if source not in cmake_path.read_text(encoding="utf-8"): raise SystemExit("Array CMake integration failed: "+source)
print("SolarCrystal 0.8.8a bounded array runtime integrated.")
