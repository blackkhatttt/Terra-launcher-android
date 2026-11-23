#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
repo = Path(__file__).resolve().parent.parent
android = root / "terra-android"
main = android / "src/main/java/com/terra/android/MainActivity.java"
source = repo / "src/main/java/com/universe/solarcrystal/RubyDungRayPicker.java"
dest = android / "src/main/java/com/universe/solarcrystal/RubyDungRayPicker.java"
if not main.is_file() or not source.is_file():
    raise SystemExit("MainActivity or CPU ray picker source is missing")
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")
text = main.read_text(encoding="utf-8")

imp = "import com.universe.solarcrystal.RubyDungRayPicker;\\n"
if "import com.universe.solarcrystal.RubyDungRayPicker;" not in text:
    anchor = "import com.universe.solarcrystal.RubyDungLegacyGLAudit;\\n"
    if anchor not in text:
        raise SystemExit("0.9.5 legacy GL audit import missing")
    text = text.replace(anchor, anchor + imp, 1)

if "RUBYDUNG_RAY_PICKER_096" not in text:
    marker = "        Button testClassReader = makeButton("
    button = """        // RUBYDUNG_RAY_PICKER_096
        Button rayPickerTest = makeButton("TEST CPU RAY PICKER",
            Color.rgb(42, 92, 105), Color.WHITE);
        LinearLayout.LayoutParams rayPickerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        rayPickerParams.topMargin = dp(10);
        page.addView(rayPickerTest, rayPickerParams);
        rayPickerTest.setOnClickListener(v -> showResult(
            "SolarCrystal 0.9.6 • CPU Ray Picker",
            RubyDungRayPicker.selfTest()
                + "\\n\\nThis is a standalone math diagnostic. It is not yet connected to RubyDung's renderer."
        ));

"""
    if marker not in text:
        raise SystemExit("MainActivity button insertion marker missing")
    text = text.replace(marker, button + marker, 1)

text = text.replace("VERSION 0.9.5", "VERSION 0.9.6").replace(
    "SOLARCRYSTAL 0.9.5", "SOLARCRYSTAL 0.9.6")
main.write_text(text, encoding="utf-8")

gradle = android / "build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    if line.strip().startswith("versionCode "):
        lines[i] = indent + "versionCode 27"
        codes += 1
    elif line.strip().startswith("versionName "):
        lines[i] = indent + "versionName '0.9.6'"
        names += 1
if codes != 1 or names != 1:
    raise SystemExit("Could not set Android version 0.9.6")
gradle.write_text("\\n".join(lines) + "\\n", encoding="utf-8")
print("SolarCrystal 0.9.6 CPU ray picker integrated.")
