#!/usr/bin/env python3
"""Integrate SolarCrystal 0.8.10c legacy JAR picker and static preflight."""
from pathlib import Path

root = Path.cwd()
repo = Path(__file__).resolve().parent.parent
android = root / "terra-android"
main = android / "src/main/java/com/terra/android/MainActivity.java"
source = repo / "tools/LegacyJarInspector.java"
dest = android / "src/main/java/com/universe/solarcrystal/LegacyJarInspector.java"

if not main.is_file():
    raise SystemExit("MainActivity.java is missing from the Android project")
if not source.is_file():
    raise SystemExit("LegacyJarInspector.java source is missing")
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

text = main.read_text(encoding="utf-8")
if "LEGACY_JAR_PICKER_0810C" not in text:
    text = text.replace(
        "import android.app.Activity;\n",
        "import android.app.Activity;\nimport android.content.Intent;\nimport android.net.Uri;\n",
        1
    )
    text = text.replace(
        "import com.universe.solarcrystal.NativeBridge;\n",
        "import com.universe.solarcrystal.NativeBridge;\n"
        "import com.universe.solarcrystal.LegacyJarInspector;\n",
        1
    )
    marker = "        Button testClassReader = makeButton("
    button = """        // LEGACY_JAR_PICKER_0810C
        Button legacyJar = makeButton(
            "INSPECT LEGACY CLIENT JAR",
            Color.rgb(42, 78, 112),
            Color.WHITE
        );
        LinearLayout.LayoutParams legacyParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(54)
        );
        legacyParams.topMargin = dp(12);
        page.addView(legacyJar, legacyParams);
        legacyJar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "application/java-archive", "application/zip", "application/octet-stream"
            });
            startActivityForResult(intent, 8103);
        });

"""
    if marker not in text:
        raise SystemExit("Could not locate MainActivity button insertion point")
    text = text.replace(marker, button + marker, 1)

    method_marker = "    private void runClassReaderTest() {"
    method = """    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 8103 || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        try {
            String report = LegacyJarInspector.inspect(this, uri);
            showResult("SolarCrystal 0.8.10c • Legacy JAR Preflight", report);
        } catch (Exception error) {
            showResult("Legacy JAR inspection failed",
                error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

"""
    if method_marker not in text:
        raise SystemExit("Could not locate MainActivity class-reader method")
    text = text.replace(method_marker, method + method_marker, 1)

text = text.replace("VERSION 0.8.10b", "VERSION 0.8.10c")
text = text.replace("SOLARCRYSTAL 0.8.10b", "SOLARCRYSTAL 0.8.10c")
main.write_text(text, encoding="utf-8")

gradle = android / "build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    stripped = line.strip()
    if stripped.startswith("versionCode "):
        lines[i] = indent + "versionCode 20"
        codes += 1
    elif stripped.startswith("versionName "):
        lines[i] = indent + "versionName '0.8.10c'"
        names += 1
if codes != 1 or names != 1:
    raise SystemExit("Could not set Android package version to 0.8.10c")
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("SolarCrystal 0.8.10c legacy JAR picker and static preflight integrated.")
