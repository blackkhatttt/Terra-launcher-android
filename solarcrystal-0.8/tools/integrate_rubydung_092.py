#!/usr/bin/env python3
from pathlib import Path
root = Path.cwd()
repo = Path(__file__).resolve().parent.parent
android = root / "terra-android"
main = android / "src/main/java/com/terra/android/MainActivity.java"
source = repo / "src/main/java/com/universe/solarcrystal/RubyDungApiInventory.java"
dest = android / "src/main/java/com/universe/solarcrystal/RubyDungApiInventory.java"
if not main.is_file() or not source.is_file():
    raise SystemExit("MainActivity or API inventory source is missing")
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")
text = main.read_text(encoding="utf-8")
if "import com.universe.solarcrystal.RubyDungApiInventory;" not in text:
    anchor = "import com.universe.solarcrystal.RubyDungPreflight;\n"
    if anchor not in text: raise SystemExit("0.9.1 import marker missing")
    text = text.replace(anchor, anchor + "import com.universe.solarcrystal.RubyDungApiInventory;\n", 1)
if "RUBYDUNG_API_INVENTORY_092" not in text:
    marker = "        Button testClassReader = makeButton("
    button = """        // RUBYDUNG_API_INVENTORY_092
        Button rubyDungApiInventory = makeButton("INSPECT RUBYDUNG API USAGE",
            Color.rgb(42, 78, 104), Color.WHITE);
        LinearLayout.LayoutParams inventoryParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        inventoryParams.topMargin = dp(10);
        page.addView(rubyDungApiInventory, inventoryParams);
        rubyDungApiInventory.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "application/java-archive", "application/zip", "application/octet-stream"
            });
            startActivityForResult(intent, 8107);
        });

"""
    if marker not in text: raise SystemExit("Button insertion marker missing")
    text = text.replace(marker, button + marker, 1)
handler = """        if (requestCode == 8107) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
            try {
                String report = RubyDungApiInventory.inspect(this, data.getData());
                showResult("SolarCrystal 0.9.2 • RubyDung API Inventory", report);
            } catch (Exception error) {
                showResult("RubyDung API inventory failed",
                    error.getClass().getSimpleName() + ": " + error.getMessage());
            }
            return;
        }
"""
if "requestCode == 8107" not in text:
    anchor = "        if (requestCode == 8106) {"
    if anchor not in text: raise SystemExit("0.9.1 result-handler marker missing")
    text = text.replace(anchor, handler + anchor, 1)
text = text.replace("VERSION 0.9.1", "VERSION 0.9.2").replace("SOLARCRYSTAL 0.9.1", "SOLARCRYSTAL 0.9.2")
main.write_text(text, encoding="utf-8")
gradle = android / "build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    if line.strip().startswith("versionCode "): lines[i] = indent + "versionCode 23"; codes += 1
    elif line.strip().startswith("versionName "): lines[i] = indent + "versionName '0.9.2'"; names += 1
if codes != 1 or names != 1: raise SystemExit("Could not set Android version 0.9.2")
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("SolarCrystal 0.9.2 API inventory integrated.")
