from pathlib import Path
source = (Path(__file__).resolve().parents[1] / "src/main/java/com/universe/solarcrystal/RubyDungApiInventory.java").read_text()
required = ["CAFEBABE", "constant-pool", "org/lwjgl/opengl/GL11.", "org/lwjgl/input/", "org/lwjgl/util/glu/", "java/awt/", "javax/swing/", "MAX_ARCHIVE_BYTES", "MAX_ENTRIES", "EXECUTION: NOT STARTED"]
missing = [x for x in required if x not in source]
if missing: raise SystemExit("Missing API inventory checks: " + ", ".join(missing))
for forbidden in ("Class.forName", "defineClass", "Runtime.getRuntime"):
    if forbidden in source: raise SystemExit("Inventory must not execute client classes")
print("RubyDung API inventory source checks passed.")
