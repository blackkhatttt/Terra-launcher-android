#!/usr/bin/env python3
"""Source-level regression checks for RubyDung preflight integration."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / "src/main/java/com/universe/solarcrystal/RubyDungPreflight.java").read_text()
required = [
    "com/mojang/rubydung/RubyDung.class", "terrain.png",
    "Highest class major", "org/lwjgl/", "org/lwjgl/opengl/",
    "org/lwjgl/openal/", "java/awt/", "MAX_ARCHIVE_BYTES",
    "EXECUTION: NOT STARTED"
]
missing = [item for item in required if item not in source]
if missing:
    raise SystemExit("Missing RubyDung preflight checks: " + ", ".join(missing))
if "Class.forName" in source or "defineClass" in source or "Runtime.getRuntime" in source:
    raise SystemExit("Preflight must not execute or define client classes")
print("RubyDung preflight source checks passed.")
