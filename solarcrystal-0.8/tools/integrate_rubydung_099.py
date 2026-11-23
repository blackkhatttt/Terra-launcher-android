#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
repo = Path(__file__).resolve().parent.parent
android = root / "terra-android"
main = android / "src/main/java/com/terra/android/MainActivity.java"
source = repo / "src/main/java/com/universe/solarcrystal/RubyDungVoxelMesher.java"
dest = android / "src/main/java/com/universe/solarcrystal/RubyDungVoxelMesher.java"
if not main.is_file() or not source.is_file():
    raise SystemExit("MainActivity or voxel mesher source is missing")
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")
text = main.read_text(encoding="utf-8")

imp = "import com.universe.solarcrystal.RubyDungVoxelMesher;\\n"
if "import com.universe.solarcrystal.RubyDungVoxelMesher;" not in text:
    anchor = "import com.universe.solarcrystal.RubyDungBlockWorld;\\n"
    if anchor not in text:
        raise SystemExit("0.9.8 block world import missing")
    text = text.replace(anchor, anchor + imp, 1)

if "RUBYDUNG_VOXEL_MESHER_099" not in text:
    marker = "        Button testClassReader = makeButton("
    button = """        // RUBYDUNG_VOXEL_MESHER_099
        Button voxelMesherTest = makeButton("TEST VOXEL MESH GENERATOR",
            Color.rgb(54, 91, 128), Color.WHITE);
        LinearLayout.LayoutParams voxelMesherParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        voxelMesherParams.topMargin = dp(10);
        page.addView(voxelMesherTest, voxelMesherParams);
        voxelMesherTest.setOnClickListener(v -> showResult(
            "SolarCrystal 0.9.9 • Voxel Mesh Generator",
            RubyDungVoxelMesher.selfTest()
                + "\\n\\nCPU geometry only: no OpenGL upload and no RubyDung renderer connection."
        ));

"""
    if marker not in text:
        raise SystemExit("MainActivity button insertion marker missing")
    text = text.replace(marker, button + marker, 1)

text = text.replace("VERSION 0.9.8", "VERSION 0.9.9").replace(
    "SOLARCRYSTAL 0.9.8", "SOLARCRYSTAL 0.9.9")
main.write_text(text, encoding="utf-8")

gradle = android / "build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    if line.strip().startswith("versionCode "):
        lines[i] = indent + "versionCode 30"
        codes += 1
    elif line.strip().startswith("versionName "):
        lines[i] = indent + "versionName '0.9.9'"
        names += 1
if codes < 1 or names < 1:
    raise SystemExit(
        f"Could not set Android version 0.9.9 "
        f"(versionCode declarations: {codes}, versionName declarations: {names})"
    )
gradle.write_text("\\n".join(lines) + "\\n", encoding="utf-8")
print("SolarCrystal 0.9.9 voxel mesher integrated.")
