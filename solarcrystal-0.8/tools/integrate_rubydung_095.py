#!/usr/bin/env python3
from pathlib import Path

root = Path.cwd()
repo = Path(__file__).resolve().parent.parent
android = root / "terra-android"
main = android / "src/main/java/com/terra/android/MainActivity.java"
source = repo / "src/main/java/com/universe/solarcrystal/RubyDungLegacyGLAudit.java"
dest = android / "src/main/java/com/universe/solarcrystal/RubyDungLegacyGLAudit.java"
if not main.is_file() or not source.is_file():
    raise SystemExit("MainActivity or legacy GL audit source is missing")
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")
text = main.read_text(encoding="utf-8")

imp = "import com.universe.solarcrystal.RubyDungLegacyGLAudit;\n"
if "import com.universe.solarcrystal.RubyDungLegacyGLAudit;" not in text:
    anchor = "import com.universe.solarcrystal.RubyDungCompatibilityPlan;\n"
    if anchor not in text:
        raise SystemExit("0.9.4 compatibility-plan import missing")
    text = text.replace(anchor, anchor + imp, 1)

if "RUBYDUNG_LEGACY_GL_AUDIT_095" not in text:
    marker = "        Button testClassReader = makeButton("
    button = """        // RUBYDUNG_LEGACY_GL_AUDIT_095
        Button rubyDungLegacyGlAudit = makeButton("AUDIT LEGACY OPENGL FEATURES",
            Color.rgb(74, 62, 112), Color.WHITE);
        LinearLayout.LayoutParams glAuditParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        glAuditParams.topMargin = dp(10);
        page.addView(rubyDungLegacyGlAudit, glAuditParams);
        rubyDungLegacyGlAudit.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "application/java-archive", "application/zip", "application/octet-stream"
            });
            startActivityForResult(intent, 8110);
        });

"""
    if marker not in text:
        raise SystemExit("MainActivity button insertion marker missing")
    text = text.replace(marker, button + marker, 1)

handler = """        if (requestCode == 8110) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
            try {
                String report = RubyDungLegacyGLAudit.inspect(this, data.getData());
                showResult("SolarCrystal 0.9.5 • Legacy OpenGL Audit", report);
            } catch (Exception error) {
                showResult("RubyDung legacy OpenGL audit failed",
                    error.getClass().getSimpleName() + ": " + error.getMessage());
            }
            return;
        }
"""
if "requestCode == 8110" not in text:
    anchor = "        if (requestCode == 8109) {"
    if anchor not in text:
        raise SystemExit("0.9.4 result handler marker missing")
    text = text.replace(anchor, handler + anchor, 1)

text = text.replace("VERSION 0.9.4", "VERSION 0.9.5").replace("SOLARCRYSTAL 0.9.4", "SOLARCRYSTAL 0.9.5")
main.write_text(text, encoding="utf-8")

gradle = android / "build.gradle"
lines = gradle.read_text(encoding="utf-8").splitlines()
codes = names = 0
for i, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    if line.strip().startswith("versionCode "):
        lines[i] = indent + "versionCode 26"
        codes += 1
    elif line.strip().startswith("versionName "):
        lines[i] = indent + "versionName '0.9.5'"
        names += 1
if codes != 1 or names != 1:
    raise SystemExit("Could not set Android version 0.9.5")
gradle.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("SolarCrystal 0.9.5 legacy OpenGL audit integrated.")
