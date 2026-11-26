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

for name in (
    "RubyDungVoxelMesher.java", "RubyDungVoxelMeshData.java",
    "RubyDungBlockWorld.java", "RubyDungVoxelBlockWorldFactory.java",
    "RubyDungVoxelPlayerController.java", "RubyDungRayPicker.java",
    "RubyDungPickingScene.java"
):
    source = repo / "src/main/java/com/universe/solarcrystal" / name
    if not source.is_file():
        raise SystemExit(f"Missing SolarCrystal 0.9.12 core source: {source}")
    (core / name).write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

for name in ("RubyDungVoxelRenderer.java", "MinecraftPreviewActivity.java"):
    source = repo / "android" / name
    if not source.is_file():
        raise SystemExit(f"Missing SolarCrystal 0.9.12 Android source: {source}")
    (app / name).write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

gradle = android / "build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    if line.strip().startswith("versionCode "):
        lines[i] = indent + "versionCode 33"
        codes += 1
    elif line.strip().startswith("versionName "):
        lines[i] = indent + "versionName '0.9.12'"
        names += 1
if codes < 1 or names < 1:
    raise SystemExit(f"Could not set Android version 0.9.12 ({codes} versionCode, {names} versionName)")
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")

manifest = android / "src/main/AndroidManifest.xml"
manifest_text = manifest.read_text(encoding="utf-8")
if "MinecraftPreviewActivity" not in manifest_text:
    raise SystemExit("MinecraftPreviewActivity is not registered")
if 'android:glEsVersion="0x00020000"' not in manifest_text:
    raise SystemExit("OpenGL ES 2.0 declaration is missing")

activity = (app / "MinecraftPreviewActivity.java").read_text(encoding="utf-8")
renderer = (app / "RubyDungVoxelRenderer.java").read_text(encoding="utf-8")
for marker in ('actionButton("BREAK")', 'actionButton("PLACE")',
               'actionButton("BLOCK")', 'actionButton("JUMP")',
               'class JoystickOverlay extends View', 'drawStick(canvas, leftCenterX',
               'drawStick(canvas, rightCenterX', 'MotionEvent.ACTION_POINTER_DOWN'):
    if marker not in activity:
        raise SystemExit(f"Missing Android touch control: {marker}")
for marker in ("void breakBlock()", "void placeBlock()", "void nextBlock()",
               "void setLookInput(float horizontal, float vertical)",
               "player.rotate(lookHorizontal", "world.pick(origin, direction, 6.0)",
               "GLES20.glBufferData"):
    if marker not in renderer:
        raise SystemExit(f"Missing voxel interaction implementation: {marker}")
print("SolarCrystal 0.9.12 touch controls and voxel block interaction integrated.")
