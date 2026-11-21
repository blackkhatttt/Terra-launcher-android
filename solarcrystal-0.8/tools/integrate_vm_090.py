#!/usr/bin/env python3
"""Integrate the in-app two-client static comparison for SolarCrystal 0.9.0."""
from pathlib import Path

root = Path.cwd()
repo = Path(__file__).resolve().parent.parent
android = root / "terra-android"
main = android / "src/main/java/com/terra/android/MainActivity.java"
source = repo / "tools/LegacyJarComparison.java"
dest = android / "src/main/java/com/universe/solarcrystal/LegacyJarComparison.java"

if not main.is_file():
    raise SystemExit("MainActivity.java is missing from the Android project")
if not source.is_file():
    raise SystemExit("LegacyJarComparison.java source is missing")

dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")
text = main.read_text(encoding="utf-8")
if "import com.universe.solarcrystal.LegacyJarComparison;" not in text:
    anchor = "import com.universe.solarcrystal.LegacyJarInspector;\\n"
    if anchor not in text:
        raise SystemExit("Could not find the legacy inspector import")
    text = text.replace(
        anchor,
        anchor + "import com.universe.solarcrystal.LegacyJarComparison;\\n",
        1
    )

if "LEGACY_JAR_PICKER_0810C" not in text or "LegacyJarInspector.inspect" not in text:
    raise SystemExit("The 0.8.10c legacy JAR inspector must be integrated first")

if "LEGACY_JAR_COMPARE_090" not in text:
    marker = "        Button testClassReader = makeButton("
    button = """        // LEGACY_JAR_COMPARE_090
        Button compareLegacyJars = makeButton(
            "COMPARE TWO LEGACY CLIENTS",
            Color.rgb(68, 58, 112),
            Color.WHITE
        );
        LinearLayout.LayoutParams compareLegacyParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(54)
        );
        compareLegacyParams.topMargin = dp(10);
        page.addView(compareLegacyJars, compareLegacyParams);
        compareLegacyJars.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "application/java-archive", "application/zip", "application/octet-stream"
            });
            startActivityForResult(intent, 8104);
        });

"""
    if marker not in text:
        raise SystemExit("Could not find the main test-button insertion point")
    text = text.replace(marker, button + marker, 1)

field = "    private Uri comparisonFirstJarUri;\n"
if field.strip() not in text:
    method_marker = "    private void runClassReaderTest() {"
    if method_marker not in text:
        raise SystemExit("Could not find class-reader method for comparison state")
    text = text.replace(method_marker, field + method_marker, 1)

old = "        if (requestCode != 8103 || resultCode != RESULT_OK || data == null) return;"
new = """        if (requestCode == 8104) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
            comparisonFirstJarUri = data.getData();
            Intent next = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            next.addCategory(Intent.CATEGORY_OPENABLE);
            next.setType("*/*");
            next.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "application/java-archive", "application/zip", "application/octet-stream"
            });
            startActivityForResult(next, 8105);
            return;
        }
        if (requestCode == 8105) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
            if (comparisonFirstJarUri == null) {
                showResult("Legacy comparison", "Select both client archives again.");
                return;
            }
            try {
                String comparison = LegacyJarComparison.compare(
                    this, comparisonFirstJarUri, data.getData()
                );
                showResult("SolarCrystal 0.9.0 • Client Comparison", comparison);
            } catch (Exception error) {
                showResult("Legacy comparison failed",
                    error.getClass().getSimpleName() + ": " + error.getMessage());
            } finally {
                comparisonFirstJarUri = null;
            }
            return;
        }
        if (requestCode != 8103 || resultCode != RESULT_OK || data == null) return;"""
if "requestCode == 8104" not in text:
    if old not in text:
        raise SystemExit("Could not find the 0.8.10c JAR picker result handler")
    text = text.replace(old, new, 1)

text = text.replace("SolarCrystal 0.8.10c • Legacy JAR Preflight",
                    "SolarCrystal 0.9.0 • Legacy JAR Preflight")
text = text.replace("VERSION 0.8.10c", "VERSION 0.9.0")
text = text.replace("SOLARCRYSTAL 0.8.10c", "SOLARCRYSTAL 0.9.0")
main.write_text(text, encoding="utf-8")

gradle = android / "build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for index, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    stripped = line.strip()
    if stripped.startswith("versionCode "):
        lines[index] = indent + "versionCode 21"
        codes += 1
    elif stripped.startswith("versionName "):
        lines[index] = indent + "versionName '0.9.0'"
        names += 1
if codes != 1 or names != 1:
    raise SystemExit("Could not set Android package version to 0.9.0")
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("SolarCrystal 0.9.0 two-client comparison integrated.")
