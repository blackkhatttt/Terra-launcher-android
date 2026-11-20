#!/usr/bin/env python3
from pathlib import Path
import shutil

root = Path.cwd()
app = root / "terra-android/src/main"
main = app / "java/com/terra/android/MainActivity.java"
if not main.is_file():
    raise SystemExit("MainActivity.java is missing")

# Package the supplied original image when it is available in the asset folder.
icon = Path(__file__).resolve().parent.parent / "assets/ic_launcher.png"
if icon.is_file():
    res = app / "res"
    out = res / "mipmap-nodpi"
    out.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(icon, out / "ic_launcher.png")
    out = res / "mipmap-anydpi-v26"
    out.mkdir(parents=True, exist_ok=True)
    (out / "ic_launcher.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '  <background android:drawable="@color/launcher_icon_background" />\n'
        '  <foreground android:drawable="@mipmap/ic_launcher" />\n'
        '</adaptive-icon>\n', encoding="utf-8")
    out = res / "values"
    out.mkdir(parents=True, exist_ok=True)
    (out / "launcher_colors.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<resources><color name="launcher_icon_background">#080D24</color></resources>\n',
        encoding="utf-8")
    manifest = app / "AndroidManifest.xml"
    m = manifest.read_text(encoding="utf-8")
    if 'android:icon="@mipmap/ic_launcher"' not in m:
        m = m.replace('android:label="Universe Launcher"',
                      'android:label="Universe Launcher"\n'
                      '        android:icon="@mipmap/ic_launcher"\n'
                      '        android:roundIcon="@mipmap/ic_launcher"')
    manifest.write_text(m, encoding="utf-8")

text = main.read_text(encoding="utf-8")
replacements = {
    "private final int background = Color.rgb(14, 22, 18);": "private final int background = Color.rgb(8, 13, 30);",
    "private final int panel = Color.rgb(26, 40, 32);": "private final int panel = Color.argb(224, 17, 28, 52);",
    "private final int green = Color.rgb(55, 190, 119);": "private final int green = Color.rgb(91, 218, 255);",
    "private final int white = Color.rgb(240, 247, 242);": "private final int white = Color.rgb(246, 249, 255);",
    "private final int muted = Color.rgb(171, 190, 178);": "private final int muted = Color.rgb(206, 214, 235);",
    "scroll.setBackgroundColor(background);": "scroll.setBackground(new SunsetVoxelDrawable());",
    "LAUNCHER  •  ANDROID EDITION  •  VERSION 0.7": "JAVA EDITION LAUNCHER  •  ANDROID  •  VERSION 0.8.9d",
    "SOLARCRYSTAL 0.7  •  CLASS LOADING & RESOLUTION": "SOLARCRYSTAL 0.8.9d  •  JAVA RUNTIME DEVELOPMENT",
}
for old, new in replacements.items():
    if old not in text:
        raise SystemExit("Missing MainActivity marker: " + old)
    text = text.replace(old, new, 1)
text = text.replace("page.addView(logo);",
    "logo.setShadowLayer(dp(8), 0, dp(2), Color.rgb(8, 13, 30));\n        page.addView(logo);", 1)
text = text.replace("Color.rgb(24, 132, 79),", "Color.rgb(255, 166, 77),", 1)
text = text.replace("button.setBackgroundTintList(\n            android.content.res.ColorStateList.valueOf(color)\n        );",
    "android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();\n"
    "        shape.setColor(color);\n        shape.setCornerRadius(dp(16));\n"
    "        shape.setStroke(dp(1), Color.argb(75, 220, 240, 255));\n"
    "        button.setBackground(shape);\n        button.setElevation(dp(2));\n"
    "        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);", 1)
text = text.replace("card.setBackgroundColor(panel);",
    "android.graphics.drawable.GradientDrawable cardShape = new android.graphics.drawable.GradientDrawable();\n"
    "        cardShape.setColor(panel);\n        cardShape.setCornerRadius(dp(18));\n"
    "        cardShape.setStroke(dp(1), Color.argb(70, 130, 210, 255));\n"
    "        card.setBackground(cardShape);", 1)
main.write_text(text, encoding="utf-8")

gradle = root / "terra-android/build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    s = line.strip()
    indent = line[:len(line)-len(line.lstrip())]
    if s.startswith("versionCode "):
        lines[i] = indent + "versionCode 15"; codes += 1
    elif s.startswith("versionName "):
        lines[i] = indent + "versionName '0.8.9d'"; names += 1
if codes != 1 or names != 1:
    raise SystemExit("Could not set Android version to 0.8.9d")
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("Universe Launcher 0.8.9d interface and voxel sunset integrated.")
print("Official icon packaged." if icon.is_file() else "Official icon asset still needs to be committed at solarcrystal-0.8/assets/ic_launcher.png.")
