#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
base = Path(__file__).resolve().parent
android = root / "terra-android/src/main"
java_dir = android / "java/com/terra/android"
java_dir.mkdir(parents=True, exist_ok=True)

for name in ("MinecraftPreviewRenderer.java", "MinecraftPreviewActivity.java"):
    source = base.parent / "android" / name
    if not source.is_file():
        raise SystemExit(f"Missing SolarCrystal 0.8.8d source: {source}")
    (java_dir / name).write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

main_activity = java_dir / "MainActivity.java"
main_text = main_activity.read_text(encoding="utf-8")
main_text = main_text.replace("VERSION 0.8.8c", "VERSION 0.8.8d")
main_text = main_text.replace("SOLARCRYSTAL 0.8.8c", "SOLARCRYSTAL 0.8.8d")
main_activity.write_text(main_text, encoding="utf-8")

gradle = root / "terra-android/build.gradle"
gradle_text = gradle.read_text(encoding="utf-8")
lines = gradle_text.splitlines()
code_count = 0
name_count = 0
for index, line in enumerate(lines):
    stripped = line.strip()
    indent = line[:len(line) - len(line.lstrip())]
    if stripped.startswith("versionCode "):
        lines[index] = indent + "versionCode 10"
        code_count += 1
    elif stripped.startswith("versionName "):
        lines[index] = indent + "versionName '0.8.8d'"
        name_count += 1
if code_count != 1 or name_count != 1:
    raise SystemExit("Could not set Android versionCode/versionName to 0.8.8d")
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")

print("SolarCrystal 0.8.8d interactive preview integrated.")
