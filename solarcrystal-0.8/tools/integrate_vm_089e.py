#!/usr/bin/env python3
"""Universe Launcher 0.8.9e: refine the launcher UI and finalize Android icon resources."""
from pathlib import Path
import base64
import shutil
import runpy

root = Path.cwd()
# Rebuild the complete 0.8.9d UI first; the CI baseline starts from 0.7.
runpy.run_path(str(Path(__file__).with_name("integrate_vm_089d.py")))
app = root / "terra-android/src/main"
java = app / "java/com/terra/android/MainActivity.java"
manifest = app / "AndroidManifest.xml"
assets = Path(__file__).resolve().parent.parent / "assets"
icon = assets / "ic_launcher.png"
encoded = assets / "ic_launcher.png.b64"

if not java.is_file() or not manifest.is_file():
    raise SystemExit("Android launcher source or manifest is missing")
if not icon.is_file() and encoded.is_file():
    icon.write_bytes(base64.b64decode(encoded.read_text(encoding="ascii").strip()))
if not icon.is_file():
    raise SystemExit("Official Universe Launcher icon asset is missing")

# Install the supplied artwork as the legacy launcher icon and adaptive foreground.
res = app / "res"
legacy = res / "mipmap-nodpi"
legacy.mkdir(parents=True, exist_ok=True)
shutil.copyfile(icon, legacy / "ic_launcher.png")
adaptive = res / "mipmap-anydpi-v26"
adaptive.mkdir(parents=True, exist_ok=True)
(adaptive / "ic_launcher.xml").write_text(
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
    '  <background android:drawable="@color/launcher_icon_background" />\n'
    '  <foreground android:drawable="@mipmap/ic_launcher" />\n'
    '</adaptive-icon>\n', encoding="utf-8")
values = res / "values"
values.mkdir(parents=True, exist_ok=True)
(values / "launcher_colors.xml").write_text(
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<resources><color name="launcher_icon_background">#080D24</color></resources>\n',
    encoding="utf-8")

m = manifest.read_text(encoding="utf-8")
if 'android:icon="@mipmap/ic_launcher"' not in m:
    m = m.replace('android:label="Universe Launcher"',
                  'android:label="Universe Launcher"\n'
                  '        android:icon="@mipmap/ic_launcher"\n'
                  '        android:roundIcon="@mipmap/ic_launcher"')
manifest.write_text(m, encoding="utf-8")

# Apply the 0.8.9e visual refinements without changing launcher actions.
text = java.read_text(encoding="utf-8")
changes = [
    ("cardShape.setColor(panel);",
     "cardShape.setColors(new int[] { Color.argb(242, 20, 36, 68), Color.argb(226, 9, 18, 42) });\n"
     "        cardShape.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.TL_BR);"),
    ("cardShape.setCornerRadius(dp(18));", "cardShape.setCornerRadius(dp(22));"),
    ("cardShape.setStroke(dp(1), Color.argb(70, 130, 210, 255));",
     "cardShape.setStroke(dp(1), Color.argb(105, 130, 210, 255));"),
    ("shape.setCornerRadius(dp(16));", "shape.setCornerRadius(dp(19));"),
    ("shape.setStroke(dp(1), Color.argb(75, 220, 240, 255));",
     "shape.setStroke(dp(1), Color.argb(115, 220, 240, 255));"),
    ("button.setElevation(dp(2));", "button.setElevation(dp(4));"),
]
for old, new in changes:
    if old not in text:
        raise SystemExit("Expected 0.8.9d UI marker missing: " + old)
    text = text.replace(old, new, 1)

# Add a subtle press feedback to the existing rounded buttons.
needle = "button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);"
replacement = needle + "\n        button.setStateListAnimator(android.animation.StateListAnimator.class.cast(button.getStateListAnimator()));"
# Keep native Android state animation untouched; the rounded gradient surfaces already provide feedback.
# Increase the header contrast and keep the existing wording and layout intact.
text = text.replace("logo.setShadowLayer(dp(8), 0, dp(2), Color.rgb(8, 13, 30));",
                    "logo.setShadowLayer(dp(11), 0, dp(3), Color.rgb(8, 13, 30));", 1)
java.write_text(text, encoding="utf-8")

gradle = root / "terra-android/build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    s = line.strip()
    indent = line[:len(line)-len(line.lstrip())]
    if s.startswith("versionCode "):
        lines[i] = indent + "versionCode 16"
        codes += 1
    elif s.startswith("versionName "):
        lines[i] = indent + "versionName '0.8.9e'"
        names += 1
if codes != 1 or names != 1:
    raise SystemExit("Could not set Android version to 0.8.9e")
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")

print("Universe Launcher 0.8.9e icon and refined interface integrated.")
