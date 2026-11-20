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
        raise SystemExit(f"Missing SolarCrystal 0.8.8c source: {source}")
    (java_dir / name).write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

activity = java_dir / "MainActivity.java"
text = activity.read_text(encoding="utf-8")
text = text.replace("VERSION 0.7", "VERSION 0.8.8c")
text = text.replace("SOLARCRYSTAL 0.7", "SOLARCRYSTAL 0.8.8c")
activity.write_text(text, encoding="utf-8")

manifest = android / "AndroidManifest.xml"
manifest_text = manifest.read_text(encoding="utf-8")
if "MinecraftPreviewActivity" not in manifest_text:
    marker = '        <activity\n            android:name=".MainActivity"'
    entry = '''        <activity
            android:name=".MinecraftPreviewActivity"
            android:exported="false"
            android:screenOrientation="landscape" />
'''
    if marker not in manifest_text:
        raise SystemExit("Manifest MainActivity declaration not found")
    manifest_text = manifest_text.replace(marker, entry + marker, 1)
if 'android:glEsVersion="0x00020000"' not in manifest_text:
    manifest_text = manifest_text.replace(
        "<application",
        '<uses-feature android:glEsVersion="0x00020000" android:required="true" />\n    <application',
        1
    )
manifest.write_text(manifest_text, encoding="utf-8")

gradle = root / "terra-android/build.gradle"
gradle_text = gradle.read_text(encoding="utf-8")
gradle_text = gradle_text.replace("versionCode 8", "versionCode 9")
gradle_text = gradle_text.replace("versionName '0.8.8b'", "versionName '0.8.8c'")
gradle.write_text(gradle_text, encoding="utf-8")

print("SolarCrystal 0.8.8c optimized voxel preview integrated.")
