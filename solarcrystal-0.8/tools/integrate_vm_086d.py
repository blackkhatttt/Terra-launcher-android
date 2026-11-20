#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
base = Path(__file__).resolve().parent
native = base.parent / "native"
cpp = root / "terra-android/src/main/cpp"
include_dir = cpp / "include"

if not include_dir.is_dir():
    raise SystemExit("Native include directory missing")

(include_dir / "class_file_method_executor.h").write_text(
    (native / "class_file_method_executor.h").read_text(encoding="utf-8"),
    encoding="utf-8",
)
(cpp / "class_file_method_executor.cpp").write_text(
    (native / "class_file_method_executor.cpp").read_text(encoding="utf-8"),
    encoding="utf-8",
)

cmake = cpp / "CMakeLists.txt"
source = cmake.read_text(encoding="utf-8")
if "class_file_method_executor.cpp" not in source:
    marker = "    class_file_execution_resolver.cpp"
    if marker not in source:
        raise SystemExit("Could not find class_file_execution_resolver.cpp in CMake source list")
    source = source.replace(marker, marker + "\n    class_file_method_executor.cpp", 1)
cmake.write_text(source, encoding="utf-8")

if "execute_class_method" not in (cpp / "class_file_method_executor.cpp").read_text(encoding="utf-8"):
    raise SystemExit("Method executor integration validation failed")
print("SolarCrystal 0.8.6d parsed method executor integrated.")
