#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
base = Path(__file__).resolve().parent
cpp = root / "terra-android/src/main/cpp"
native = base.parent / "native"

include_dir = cpp / "include"
if not include_dir.is_dir():
    raise SystemExit("Native include directory missing")

(include_dir / "vm_value.h").write_text(
    (native / "vm_value.h").read_text(encoding="utf-8"),
    encoding="utf-8",
)
(cpp / "vm_value.cpp").write_text(
    (native / "vm_value.cpp").read_text(encoding="utf-8"),
    encoding="utf-8",
)

cmake = cpp / "CMakeLists.txt"
source = cmake.read_text(encoding="utf-8")
if "vm_value.cpp" not in source:
    marker = "    object_heap.cpp"
    if marker not in source:
        raise SystemExit("Could not locate object_heap.cpp in native CMake source list")
    source = source.replace(marker, marker + "\n    vm_value.cpp", 1)
    cmake.write_text(source, encoding="utf-8")

if "VmFrame::duplicate_top" not in (cpp / "vm_value.cpp").read_text(encoding="utf-8"):
    raise SystemExit("VM value runtime validation failed")

print("SolarCrystal 0.8.4b tagged value runtime integrated.")
