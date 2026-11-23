#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
repo = Path(__file__).resolve().parent.parent
android = root / "terra-android"
main = android / "src/main/java/com/terra/android/MainActivity.java"
source = repo / "src/main/java/com/universe/solarcrystal/RubyDungPickingScene.java"
dest = android / "src/main/java/com/universe/solarcrystal/RubyDungPickingScene.java"
if not main.is_file() or not source.is_file():
    raise SystemExit("MainActivity or picking scene source is missing")
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")
text = main.read_text(encoding="utf-8")

imp = "import com.universe.solarcrystal.RubyDungPickingScene;\n"
if "import com.universe.solarcrystal.RubyDungPickingScene;" not in text:
    anchor = "import com.universe.solarcrystal.RubyDungRayPicker;\n"
    if anchor not in text:
        raise SystemExit("0.9.6 ray picker import missing")
    text = text.replace(anchor, anchor + imp, 1)

if "RUBYDUNG_PICKING_SCENE_097" not in text:
    marker = "        Button testClassReader = makeButton("
    button = """        // RUBYDUNG_PICKING_SCENE_097
        Button pickingSceneTest = makeButton("TEST OBJECT PICKING SCENE",
            Color.rgb(48, 104, 82), Color.WHITE);
        LinearLayout.LayoutParams pickingSceneParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        pickingSceneParams.topMargin = dp(10);
        page.addView(pickingSceneTest, pickingSceneParams);
        pickingSceneTest.setOnClickListener(v -> showResult(
            "SolarCrystal 0.9.7 • Object Picking Scene",
            RubyDungPickingScene.selfTest()
                + "\n\nThis is an independent selection-layer test; it is not wired into RubyDung's renderer or world."
        ));

"""
    if marker not in text:
        raise SystemExit("MainActivity button insertion marker missing")
    text = text.replace(marker, button + marker, 1)

text = text.replace("VERSION 0.9.6", "VERSION 0.9.7").replace(
    "SOLARCRYSTAL 0.9.6", "SOLARCRYSTAL 0.9.7")
main.write_text(text, encoding="utf-8")

gradle = android / "build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    if line.strip().startswith("versionCode "):
        lines[i] = indent + "versionCode 28"
        codes += 1
    elif line.strip().startswith("versionName "):
        lines[i] = indent + "versionName '0.9.7'"
        names += 1
if codes != 1 or names != 1:
    raise SystemExit("Could not set Android version 0.9.7")
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("SolarCrystal 0.9.7 object picking scene integrated.")
