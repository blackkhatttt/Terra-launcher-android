#!/usr/bin/env python3
"""Integrate SolarCrystal 0.8.10b core-library registry expansion."""
from pathlib import Path

root = Path.cwd()
repo = Path(__file__).resolve().parent.parent
app = root / "terra-android/src/main"
native = repo / "native"
for name in ("core_library_registry.h", "core_library_registry.cpp"):
    source = native / name
    if not source.is_file():
        raise SystemExit(f"Missing core library source: {source}")
    (app / "cpp" / name).write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

gradle = root / "terra-android/build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    stripped = line.strip()
    if stripped.startswith("versionCode "):
        lines[i] = indent + "versionCode 19"
        codes += 1
    elif stripped.startswith("versionName "):
        lines[i] = indent + "versionName '0.8.10b'"
        names += 1
if codes != 1 or names != 1:
    raise SystemExit("Could not set Android version to 0.8.10b")
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("SolarCrystal 0.8.10b core-library registry integrated.")
