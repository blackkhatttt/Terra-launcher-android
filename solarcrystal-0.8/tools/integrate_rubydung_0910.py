#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
repo = Path(__file__).resolve().parent.parent
android = root / "terra-android"
java_root = android / "src/main/java"
core = java_root / "com/universe/solarcrystal"
app = java_root / "com/terra/android"
core.mkdir(parents=True, exist_ok=True)
app.mkdir(parents=True, exist_ok=True)

for name in ("RubyDungVoxelMesher.java", "RubyDungVoxelMeshData.java",
             "RubyDungBlockWorld.java", "RubyDungVoxelBlockWorldFactory.java"):
    source = repo / "src/main/java/com/universe/solarcrystal" / name
    if not source.is_file():
        raise SystemExit(f"Missing SolarCrystal 0.9.10 core source: {source}")
    (core / name).write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

for name in ("RubyDungVoxelRenderer.java", "MinecraftPreviewActivity.java"):
    source = repo / "android" / name
    if not source.is_file():
        raise SystemExit(f"Missing SolarCrystal 0.9.10 Android source: {source}")
    (app / name).write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

main = app / "MainActivity.java"
text = main.read_text(encoding="utf-8")
text = text.replace("OPEN FIRST MINECRAFT GRAPHICS", "OPEN RUBYDUNG VOXEL RENDERER")
text = text.replace("VERSION 0.9.9", "VERSION 0.9.10")
text = text.replace("SOLARCRYSTAL 0.9.9", "SOLARCRYSTAL 0.9.10")
main.write_text(text, encoding="utf-8")

gradle = android / "build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    if line.strip().startswith("versionCode "):
        lines[i] = indent + "versionCode 31"
        codes += 1
    elif line.strip().startswith("versionName "):
        lines[i] = indent + "versionName '0.9.10'"
        names += 1
if codes < 1 or names < 1:
    raise SystemExit(
        f"Could not set Android version 0.9.10 "
        f"(versionCode declarations: {codes}, versionName declarations: {names})"
    )
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")

manifest = android / "src/main/AndroidManifest.xml"
manifest_text = manifest.read_text(encoding="utf-8")
if "MinecraftPreviewActivity" not in manifest_text:
    raise SystemExit("MinecraftPreviewActivity is not registered in AndroidManifest.xml")
if 'android:glEsVersion="0x00020000"' not in manifest_text:
    raise SystemExit("OpenGL ES 2.0 device feature declaration is missing")

print("SolarCrystal 0.9.10 OpenGL ES voxel renderer integrated.")
