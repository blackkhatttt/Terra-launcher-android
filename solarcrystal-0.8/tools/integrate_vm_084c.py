#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
base = Path(__file__).resolve().parent
cpp = root / "terra-android/src/main/cpp"
native = base.parent / "native"
include_dir = cpp / "include"

if not include_dir.is_dir():
    raise SystemExit("Native include directory missing")
if not (cpp / "vm_value.cpp").is_file():
    raise SystemExit("0.8.4b tagged VM values must be integrated first")

(include_dir / "reference_bytecode_executor.h").write_text(
    (native / "reference_bytecode_executor.h").read_text(encoding="utf-8"),
    encoding="utf-8",
)
(cpp / "reference_bytecode_executor.cpp").write_text(
    (native / "reference_bytecode_executor.cpp").read_text(encoding="utf-8"),
    encoding="utf-8",
)

cmake = cpp / "CMakeLists.txt"
source = cmake.read_text(encoding="utf-8")
if "reference_bytecode_executor.cpp" not in source:
    marker = "    vm_value.cpp"
    if marker not in source:
        raise SystemExit("Could not locate vm_value.cpp in native CMake source list")
    source = source.replace(
        marker, marker + "\n    reference_bytecode_executor.cpp", 1
    )
    cmake.write_text(source, encoding="utf-8")

integrated = (cpp / "reference_bytecode_executor.cpp").read_text(encoding="utf-8")
if "execute_reference_bytecode" not in integrated:
    raise SystemExit("Reference bytecode executor integration validation failed")

print("SolarCrystal 0.8.4c reference bytecode executor integrated.")
