#!/usr/bin/env python3
from pathlib import Path
root=Path.cwd()
base=Path(__file__).resolve().parent
native=base.parent/"native"
cpp=root/"terra-android/src/main/cpp"
include_dir=cpp/"include"
if not include_dir.is_dir(): raise SystemExit("Native include directory missing")
(include_dir/"class_file_model.h").write_text((native/"class_file_model.h").read_text(encoding="utf-8"),encoding="utf-8")
(cpp/"class_file_model.cpp").write_text((native/"class_file_model.cpp").read_text(encoding="utf-8"),encoding="utf-8")
main=root/"terra-android/src/main/java/com/terra/android/MainActivity.java"
if main.is_file():
 text=main.read_text(encoding="utf-8").replace("0.8.9a","0.8.9b")
 main.write_text(text,encoding="utf-8")
gradle=root/"terra-android/build.gradle"
lines=gradle.read_text(encoding="utf-8").splitlines()
codes=names=0
for i,line in enumerate(lines):
 stripped=line.strip(); indent=line[:len(line)-len(line.lstrip())]
 if stripped.startswith("versionCode "): lines[i]=indent+"versionCode 12"; codes+=1
 elif stripped.startswith("versionName "): lines[i]=indent+"versionName '0.8.9b'"; names+=1
if codes!=1 or names!=1: raise SystemExit("Could not update Android version metadata to 0.8.9b")
gradle.write_text("\n".join(lines)+"\n",encoding="utf-8")
if "class_file_model.cpp" not in (cpp/"CMakeLists.txt").read_text(encoding="utf-8"): raise SystemExit("Class-file model source is not registered in CMake")
print("SolarCrystal 0.8.9b ConstantValue and class initializer metadata integrated.")
