#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
repo = Path(__file__).resolve().parent.parent
android = root / "terra-android"
main = android / "src/main/java/com/terra/android/MainActivity.java"
source = repo / "src/main/java/com/universe/solarcrystal/RubyDungBlockWorld.java"
dest = android / "src/main/java/com/universe/solarcrystal/RubyDungBlockWorld.java"
if not main.is_file() or not source.is_file():
    raise SystemExit("MainActivity or block world source is missing")
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")
text = main.read_text(encoding="utf-8")

imp = "import com.universe.solarcrystal.RubyDungBlockWorld;\n"
if "import com.universe.solarcrystal.RubyDungBlockWorld;" not in text:
    anchor = "import com.universe.solarcrystal.RubyDungPickingScene;\n"
    if anchor not in text:
        raise SystemExit("0.9.7 picking scene import missing")
    text = text.replace(anchor, anchor + imp, 1)

if "RUBYDUNG_BLOCK_WORLD_098" not in text:
    marker = "        Button testClassReader = makeButton("
    button = """        // RUBYDUNG_BLOCK_WORLD_098
        Button blockWorldTest = makeButton("TEST VOXEL BLOCK WORLD",
            Color.rgb(54, 91, 128), Color.WHITE);
        LinearLayout.LayoutParams blockWorldParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        blockWorldParams.topMargin = dp(10);
        page.addView(blockWorldTest, blockWorldParams);
        blockWorldTest.setOnClickListener(v -> showResult(
            "SolarCrystal 0.9.8 • Voxel Block World",
            RubyDungBlockWorld.selfTest()
                + "\n\nThis is a standalone voxel data and picking model; it is not connected to RubyDung's renderer or game loop."
        ));

"""
    if marker not in text:
        raise SystemExit("MainActivity button insertion marker missing")
    text = text.replace(marker, button + marker, 1)

text = text.replace("VERSION 0.9.7", "VERSION 0.9.8").replace(
    "SOLARCRYSTAL 0.9.7", "SOLARCRYSTAL 0.9.8")
main.write_text(text, encoding="utf-8")

gradle = android / "build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    if line.strip().startswith("versionCode "):
        lines[i] = indent + "versionCode 29"
        codes += 1
    elif line.strip().startswith("versionName "):
        lines[i] = indent + "versionName '0.9.8'"
        names += 1
if codes < 1 or names < 1:
    raise SystemExit(
        f"Could not set Android version 0.9.8 "
        f"(versionCode declarations: {codes}, versionName declarations: {names})"
    )
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("SolarCrystal 0.9.8 voxel block world integrated.")
