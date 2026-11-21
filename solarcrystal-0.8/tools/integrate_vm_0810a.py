#!/usr/bin/env python3
"""Integrate SolarCrystal 0.8.10a core Java library registry and verified icon resources."""
from pathlib import Path
import base64
import re
import shutil

root = Path.cwd()
repo = Path(__file__).resolve().parent.parent
app = root / "terra-android/src/main"
native = repo / "native"

# Native class-library inventory used by dependency analysis and preflight.
for name in ("core_library_registry.h", "core_library_registry.cpp"):
    source = native / name
    if not source.is_file():
        raise SystemExit(f"Missing SolarCrystal library source: {source}")
    shutil.copy2(source, app / "cpp" / name)

cmake = app / "cpp/CMakeLists.txt"
text = cmake.read_text(encoding="utf-8")
if "core_library_registry.cpp" not in text:
    text = text.replace("add_library(", "add_library(", 1)
    match = re.search(r"add_library\\s*\\([^\\n]*\\n", text)
    if not match:
        raise SystemExit("Could not locate native add_library source list")
    text = text[:match.end()] + "    core_library_registry.cpp\\n" + text[match.end():]
    cmake.write_text(text, encoding="utf-8")

java_source = repo / "src/main/java/com/universe/solarcrystal/JavaLibraryBridge.java"
java_dest = app / "java/com/universe/solarcrystal/JavaLibraryBridge.java"
java_dest.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(java_source, java_dest)

# Use the exact supplied galaxy-U artwork stored as PNG base64.
assets = repo / "assets"
encoded = assets / "ic_launcher.png.b64"
if not encoded.is_file():
    raise SystemExit("Missing supplied Universe Launcher icon: assets/ic_launcher.png.b64")
try:
    icon_bytes = base64.b64decode(encoded.read_text(encoding="ascii").strip(), validate=True)
except Exception as exc:
    raise SystemExit(f"Invalid icon base64 asset: {exc}")
if not icon_bytes.startswith(b"\\x89PNG\\r\\n\\x1a\\n"):
    raise SystemExit("Universe Launcher icon is not a valid PNG")

mipmap = app / "res/mipmap-nodpi"
drawable = app / "res/drawable-nodpi"
adaptive = app / "res/mipmap-anydpi-v26"
for folder in (mipmap, drawable, adaptive):
    folder.mkdir(parents=True, exist_ok=True)
for stale in ("ic_launcher.jpg", "ic_launcher.jpeg", "ic_launcher.webp"):
    path = mipmap / stale
    if path.exists():
        path.unlink()
(mipmap / "ic_launcher.png").write_bytes(icon_bytes)
(drawable / "ic_launcher_foreground.png").write_bytes(icon_bytes)
(adaptive / "ic_launcher.xml").write_text(
    '<?xml version="1.0" encoding="utf-8"?>\\n'
    '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\\n'
    '  <background android:drawable="@color/launcher_icon_background" />\\n'
    '  <foreground android:drawable="@drawable/ic_launcher_foreground" />\\n'
    '</adaptive-icon>\\n', encoding="utf-8")
values = app / "res/values"
values.mkdir(parents=True, exist_ok=True)
(values / "launcher_colors.xml").write_text(
    '<?xml version="1.0" encoding="utf-8"?>\\n'
    '<resources><color name="launcher_icon_background">#080D24</color></resources>\\n',
    encoding="utf-8")

manifest = app / "AndroidManifest.xml"
if not manifest.is_file():
    raise SystemExit("AndroidManifest.xml is missing")
manifest_text = manifest.read_text(encoding="utf-8")
start = manifest_text.find("<application")
end = manifest_text.find(">", start)
if start < 0 or end < 0:
    raise SystemExit("Could not locate Android application declaration")
head = manifest_text[start:end]
for attribute in ("icon", "roundIcon"):
    pattern = rf'android:{attribute}="[^"]+"'
    replacement = f'android:{attribute}="@mipmap/ic_launcher"'
    if re.search(pattern, head):
        head = re.sub(pattern, replacement, head, count=1)
    else:
        head += f'\\n        android:{attribute}="@mipmap/ic_launcher"'
manifest_text = manifest_text[:start] + head + manifest_text[end:]
manifest.write_text(manifest_text, encoding="utf-8")

gradle = root / "terra-android/build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    stripped = line.strip()
    if stripped.startswith("versionCode "):
        lines[i] = indent + "versionCode 18"
        codes += 1
    elif stripped.startswith("versionName "):
        lines[i] = indent + "versionName '0.8.10a'"
        names += 1
if codes != 1 or names != 1:
    raise SystemExit("Could not set Android version to 0.8.10a")
gradle.write_text("\\n".join(lines) + "\\n", encoding="utf-8")
print("SolarCrystal 0.8.10a core-library registry, host bridge and PNG/adaptive icon integrated.")
