#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
base = Path(__file__).resolve().parent
cpp = root / "terra-android/src/main/cpp"
include_dir = cpp / "include"
java = root / "terra-android/src/main/java/com/universe/solarcrystal/NativeBridge.java"

if not include_dir.is_dir():
    raise SystemExit("Native include directory missing")
if not java.is_file():
    raise SystemExit("NativeBridge.java is missing")

source = cpp / "jni_bridge.cpp"
if not source.is_file():
    raise SystemExit("jni_bridge.cpp is missing")
text = source.read_text(encoding="utf-8")
marker = '#include "class_file_model.h"'
if marker not in text:
    text = text.replace('#include "class_method_executor.h"', '#include "class_method_executor.h"\n' + marker)
new_function = (base.parent / "native/jni_static_metadata_bridge.cpp").read_text(encoding="utf-8")
if "Java_com_universe_solarcrystal_NativeBridge_nativeInspectStaticMetadata" not in text:
    text += "\n" + new_function
source.write_text(text, encoding="utf-8")

java_text = java.read_text(encoding="utf-8")
declaration = """    /** Reads static-field metadata through the native class-file model. */
    public static native String nativeInspectStaticMetadata(byte[] classBytes);
"""
if "nativeInspectStaticMetadata" not in java_text:
    java_text = java_text.replace("    public static native String nativeInspectClass(", declaration + "\n    public static native String nativeInspectClass(")
java.write_text(java_text, encoding="utf-8")

cmake = cpp / "CMakeLists.txt"
cmake_text = cmake.read_text(encoding="utf-8")
if "class_file_model.cpp" not in cmake_text:
    raise SystemExit("class_file_model.cpp must be registered before JNI bridge integration")
if "jni_bridge.cpp" not in cmake_text:
    raise SystemExit("Existing JNI bridge source was not found in CMake")
if "class_file_model.cpp" not in cmake_text:
    raise SystemExit("Class-file model source missing from CMake")
if "jni_static_metadata_bridge.cpp" not in cmake_text:
    # The new JNI entry point is appended to the already-registered JNI translation unit.
    pass

gradle = root / "terra-android/build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for index, line in enumerate(lines):
    stripped = line.strip()
    indent = line[:len(line) - len(line.lstrip())]
    if stripped.startswith("versionCode "):
        lines[index] = indent + "versionCode 14"
        codes += 1
    elif stripped.startswith("versionName "):
        lines[index] = indent + "versionName '0.8.9c'"
        names += 1
if codes != 1 or names != 1:
    raise SystemExit("Could not update Android version metadata to 0.8.9c")
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")

print("SolarCrystal 0.8.9c JNI static-metadata bridge integrated.")
