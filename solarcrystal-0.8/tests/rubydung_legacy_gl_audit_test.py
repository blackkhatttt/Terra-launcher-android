from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / "src/main/java/com/universe/solarcrystal/RubyDungLegacyGLAudit.java").read_text()
required = [
    "RubyDungInvocationAudit.inspect",
    "glSelectBuffer",
    "glRenderMode",
    "glNewList",
    "glCallList",
    "CPU PICKING",
    "BUFFERED GEOMETRY",
    "FIXED FUNCTION STATE",
    "EXECUTION: NOT STARTED",
]
missing = [item for item in required if item not in source]
if missing:
    raise SystemExit("Missing legacy GL audit feature(s): " + ", ".join(missing))
for forbidden in ("Class.forName", "defineClass", "Runtime.getRuntime"):
    if forbidden in source:
        raise SystemExit("Legacy GL audit must not execute client classes")
print("RubyDung legacy OpenGL audit source checks passed.")
