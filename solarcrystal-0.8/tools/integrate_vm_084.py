#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
base = Path(__file__).resolve().parent
cpp = root / "terra-android/src/main/cpp"
native = base.parent / "native"

header = cpp / "include/object_heap.h"
source = cpp / "object_heap.cpp"
if not header.parent.exists():
    raise SystemExit("Native include directory missing")
header.write_text((native / "object_heap.h").read_text(encoding="utf-8"), encoding="utf-8")
source.write_text((native / "object_heap.cpp").read_text(encoding="utf-8"), encoding="utf-8")

cmake = cpp / "CMakeLists.txt"
text = cmake.read_text(encoding="utf-8")
if "object_heap.cpp" not in text:
    marker = "    vm_runtime.cpp"
    if marker not in text:
        raise SystemExit("Could not locate vm_runtime.cpp in native CMake source list")
    text = text.replace(marker, "    object_heap.cpp\n" + marker, 1)
    cmake.write_text(text, encoding="utf-8")

if "ObjectHeap::allocate" not in source.read_text(encoding="utf-8"):
    raise SystemExit("Object heap source validation failed")
print("SolarCrystal 0.8.4 object heap integrated.")
