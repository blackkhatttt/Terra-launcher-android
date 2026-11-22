from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / "src/main/java/com/universe/solarcrystal/RubyDungCompatibilityPlan.java").read_text()
required = [
    "RubyDungInvocationAudit.inspect",
    "DISPLAY BACKEND",
    "FIXED-FUNCTION OPENGL",
    "ANDROID INPUT BRIDGE",
    "GLU MATH HELPERS",
    "DIRECT BUFFER SUPPORT",
    "ANDROID BITMAP BRIDGE",
    "ANDROID DIALOG BRIDGE",
    "EXECUTION: NOT STARTED",
]
missing = [item for item in required if item not in source]
if missing:
    raise SystemExit("Missing compatibility-plan feature(s): " + ", ".join(missing))
for forbidden in ("Class.forName", "defineClass", "Runtime.getRuntime"):
    if forbidden in source:
        raise SystemExit("Compatibility report must not execute client classes")
print("RubyDung compatibility plan source checks passed.")
