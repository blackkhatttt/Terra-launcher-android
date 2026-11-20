#!/usr/bin/env python3
from pathlib import Path
root=Path.cwd()
base=Path(__file__).resolve().parent
native=base.parent/"native"
cpp=root/"terra-android/src/main/cpp"
include_dir=cpp/"include"
if not include_dir.is_dir(): raise SystemExit("Native include directory missing")
for name in ("class_file_linkage_resolver.h",):
 (include_dir/name).write_text((native/name).read_text(encoding="utf-8"),encoding="utf-8")
for name in ("class_file_linkage_resolver.cpp",):
 (cpp/name).write_text((native/name).read_text(encoding="utf-8"),encoding="utf-8")
cmake_path=cpp/"CMakeLists.txt"
cmake=cmake_path.read_text(encoding="utf-8")
if "class_file_linkage_resolver.cpp" not in cmake:
 lines=cmake.splitlines()
 insertion=None
 for i,line in enumerate(lines):
  if "class_method_resolver.cpp" in line:
   insertion=i+1
   break
 if insertion is None: raise SystemExit("Could not find class method resolver source in CMakeLists.txt")
 lines.insert(insertion,"    class_file_linkage_resolver.cpp")
 cmake_path.write_text("\n".join(lines)+"\n",encoding="utf-8")
if "class_file_linkage_resolver.cpp" not in cmake_path.read_text(encoding="utf-8"):
 raise SystemExit("Class-file linkage resolver CMake integration failed")
print("SolarCrystal 0.8.7d multi-class member linkage integrated.")
