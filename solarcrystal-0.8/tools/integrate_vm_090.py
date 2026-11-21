#!/usr/bin/env python3
"""Set the Android package version for the SolarCrystal 0.9.0 milestone."""
from pathlib import Path

root = Path.cwd()
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
        lines[i] = indent + "versionName '0.9.0'"
        names += 1
if codes != 1 or names != 1:
    raise SystemExit("Could not set Android package version to 0.9.0")
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")
main = root / "terra-android/src/main/java/com/terra/android/MainActivity.java"
if main.is_file():
    text = main.read_text(encoding="utf-8")
    text = text.replace("0.8.10a", "0.9.0")
    main.write_text(text, encoding="utf-8")
print("SolarCrystal 0.9.0 Android version metadata integrated.")
