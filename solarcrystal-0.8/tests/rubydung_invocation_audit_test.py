from pathlib import Path
source=(Path(__file__).resolve().parents[1]/"src/main/java/com/universe/solarcrystal/RubyDungInvocationAudit.java").read_text()
required=["0xb6","0xb7","0xb8","0xb9","Code","tableswitch","lookupswitch","wide","MAX_ARCHIVE_BYTES","MAX_ENTRIES","EXECUTION: NOT STARTED"]
missing=[x for x in required if x not in source]
if missing: raise SystemExit("Missing bytecode audit feature(s): "+", ".join(missing))
for forbidden in ("Class.forName","defineClass","Runtime.getRuntime"):
    if forbidden in source: raise SystemExit("Audit must not execute client classes")
print("RubyDung invocation audit source checks passed.")
