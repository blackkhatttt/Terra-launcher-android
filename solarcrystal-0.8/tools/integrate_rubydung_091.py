#!/usr/bin/env python3
"""Integrate the RubyDung-specific static preflight screen for SolarCrystal 0.9.1."""
from pathlib import Path

root = Path.cwd()
repo = Path(__file__).resolve().parent.parent
android = root / "terra-android"
main = android / "src/main/java/com/terra/android/MainActivity.java"
source = repo / "src/main/java/com/universe/solarcrystal/RubyDungPreflight.java"
dest = android / "src/main/java/com/universe/solarcrystal/RubyDungPreflight.java"
if not main.is_file() or not source.is_file():
    raise SystemExit("MainActivity or RubyDungPreflight source is missing")
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")
text = main.read_text(encoding="utf-8")
if "import com.universe.solarcrystal.RubyDungPreflight;" not in text:
    anchor = "import com.universe.solarcrystal.LegacyJarComparison;\n"
    if anchor not in text:
        raise SystemExit("Could not find the 0.9.0 comparison import")
    text = text.replace(anchor, anchor + "import com.universe.solarcrystal.RubyDungPreflight;\n", 1)

if "RUBYDUNG_PREFLIGHT_091" not in text:
    marker = "        Button testClassReader = makeButton("
    button = """        // RUBYDUNG_PREFLIGHT_091
        Button rubyDungPreflight = makeButton(
            "PREPARE RUBYDUNG TEST",
            Color.rgb(88, 68, 36),
            Color.WHITE
        );
        LinearLayout.LayoutParams rubyParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(54)
        );
        rubyParams.topMargin = dp(10);
        page.addView(rubyDungPreflight, rubyParams);
        rubyDungPreflight.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "application/java-archive", "application/zip", "application/octet-stream"
            });
            startActivityForResult(intent, 8106);
        });

"""
    if marker not in text:
        raise SystemExit("Could not find the main test-button insertion point")
    text = text.replace(marker, button + marker, 1)

handler = """        if (requestCode == 8106) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
            try {
                String report = RubyDungPreflight.inspect(this, data.getData());
                showResult("SolarCrystal 0.9.1 • RubyDung Preflight", report);
            } catch (Exception error) {
                showResult("RubyDung preflight failed",
                    error.getClass().getSimpleName() + ": " + error.getMessage());
            }
            return;
        }
"""
if "requestCode == 8106" not in text:
    anchor = "        if (requestCode == 8104) {"
    if anchor not in text:
        raise SystemExit("Could not find the 0.9.0 comparison result handler")
    text = text.replace(anchor, handler + anchor, 1)

text = text.replace("VERSION 0.9.0", "VERSION 0.9.1")
text = text.replace("SOLARCRYSTAL 0.9.0", "SOLARCRYSTAL 0.9.1")
main.write_text(text, encoding="utf-8")

gradle = android / "build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    stripped = line.strip()
    if stripped.startswith("versionCode "):
        lines[i] = indent + "versionCode 22"
        codes += 1
    elif stripped.startswith("versionName "):
        lines[i] = indent + "versionName '0.9.1'"
        names += 1
if codes != 1 or names != 1:
    raise SystemExit("Could not set Android package version to 0.9.1")
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("SolarCrystal 0.9.1 RubyDung-specific static preflight integrated.")
