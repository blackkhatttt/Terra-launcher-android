#!/usr/bin/env python3
"""Universe Launcher 0.8.9f: official artwork, versioning and Infdev preflight hooks."""
from pathlib import Path
import base64
import shutil

root = Path.cwd()
app = root / "terra-android/src/main"
manifest = app / "AndroidManifest.xml"
assets = Path(__file__).resolve().parent.parent / "assets"
icon_jpg = assets / "ic_launcher.jpg"
icon_b64 = assets / "ic_launcher.jpg.b64"

if not icon_jpg.is_file() and icon_b64.is_file():
    icon_jpg.write_bytes(base64.b64decode(icon_b64.read_text(encoding="ascii").strip()))
if not icon_jpg.is_file():
    raise SystemExit("Official Universe Launcher artwork is missing: assets/ic_launcher.jpg")

# JPEG is a supported Android mipmap resource and preserves the supplied artwork.
mipmap = app / "res/mipmap-nodpi"
mipmap.mkdir(parents=True, exist_ok=True)
# Android treats files with the same resource basename as duplicates even when
# their extensions differ. Remove stale variants from the 0.7 baseline first.
for stale in ("ic_launcher.png", "ic_launcher.webp", "ic_launcher.jpeg"):
    stale_path = mipmap / stale
    if stale_path.exists():
        stale_path.unlink()
shutil.copyfile(icon_jpg, mipmap / "ic_launcher.jpg")

adaptive = app / "res/mipmap-anydpi-v26"
adaptive.mkdir(parents=True, exist_ok=True)
(adaptive / "ic_launcher.xml").write_text(
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
    '  <background android:drawable="@color/launcher_icon_background" />\n'
    '  <foreground android:drawable="@mipmap/ic_launcher" />\n'
    '</adaptive-icon>\n', encoding="utf-8")
values = app / "res/values"
values.mkdir(parents=True, exist_ok=True)
(values / "launcher_colors.xml").write_text(
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<resources><color name="launcher_icon_background">#080D24</color></resources>\n',
    encoding="utf-8")

if not manifest.is_file():
    raise SystemExit("AndroidManifest.xml is missing")
m = manifest.read_text(encoding="utf-8")
application = m.find("<application")
if application < 0:
    raise SystemExit("Android application declaration is missing")
import re
if re.search(r'android:icon="[^"]+"', m[application:m.find(">", application)]):
    m = re.sub(r'android:icon="[^"]+"', 'android:icon="@mipmap/ic_launcher"', m, count=1)
else:
    m = m.replace("<application", '<application\n        android:icon="@mipmap/ic_launcher"', 1)
if re.search(r'android:roundIcon="[^"]+"', m[application:m.find(">", application)]):
    m = re.sub(r'android:roundIcon="[^"]+"', 'android:roundIcon="@mipmap/ic_launcher"', m, count=1)
else:
    m = m.replace('android:icon="@mipmap/ic_launcher"',
                  'android:icon="@mipmap/ic_launcher"\n        android:roundIcon="@mipmap/ic_launcher"', 1)
manifest.write_text(m, encoding="utf-8")

gradle = root / "terra-android/build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    s = line.strip()
    indent = line[:len(line)-len(line.lstrip())]
    if s.startswith("versionCode "):
        lines[i] = indent + "versionCode 17"
        codes += 1
    elif s.startswith("versionName "):
        lines[i] = indent + "versionName '0.8.9f'"
        names += 1
if codes != 1 or names != 1:
    raise SystemExit("Could not set Android version to 0.8.9f")
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")

print("Universe Launcher 0.8.9f official icon, adaptive icon and version integrated.")
