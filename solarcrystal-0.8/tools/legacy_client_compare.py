#!/usr/bin/env python3
"""Compare legacy Minecraft-family client JARs for SolarCrystal Android readiness."""
from pathlib import Path
from zipfile import ZipFile, BadZipFile
import argparse
import re
import sys

ENTRYPOINTS = (
    "com/mojang/rubydung/RubyDung.class",
    "net/minecraft/client/Minecraft.class",
)
TOKENS = {
    "LWJGL2": (b"org/lwjgl/",),
    "OpenGL": (b"org/lwjgl/opengl/", b"GL11"),
    "OpenAL": (b"org/lwjgl/openal/", b"AL10"),
    "AWT": (b"java/awt/", b"javax/swing/"),
    "Applet": (b"java/applet/", b"Applet"),
    "Keyboard/Mouse": (b"org/lwjgl/input/Keyboard", b"org/lwjgl/input/Mouse"),
    "Controllers": (b"org/lwjgl/input/Controllers",),
}

def inspect(path: Path):
    try:
        with ZipFile(path) as jar:
            bad = jar.testzip()
            if bad:
                raise ValueError(f"CRC failure: {bad}")
            files = [i for i in jar.infolist() if not i.is_dir()]
            names = {i.filename for i in files}
            classes = [i for i in files if i.filename.endswith(".class")]
            if not classes:
                raise ValueError("No .class files found")
            versions, features = set(), set()
            for item in classes:
                data = jar.read(item)
                if len(data) < 8 or data[:4] != b"\xca\xfe\xba\xbe":
                    raise ValueError(f"Invalid class header: {item.filename}")
                versions.add((int.from_bytes(data[6:8], "big"),
                              int.from_bytes(data[4:6], "big")))
                for feature, patterns in TOKENS.items():
                    if any(token in data for token in patterns):
                        features.add(feature)
            entrypoints = [name for name in ENTRYPOINTS if name in names]
            native = [n for n in names if n.lower().endswith((".so", ".dll", ".dylib"))]
            return {
                "path": path.name, "entries": len(files), "classes": len(classes),
                "versions": sorted(versions), "entrypoints": entrypoints,
                "features": sorted(features), "native": native,
                "resources": sum(not n.endswith(".class") for n in names),
            }
    except (BadZipFile, OSError) as exc:
        raise ValueError(str(exc)) from exc

def rank(report):
    # Fewer desktop-only APIs and fewer classes are more promising for a first Android port.
    score = report["classes"] + 100 * ("AWT" in report["features"]) \
        + 80 * ("Applet" in report["features"]) + 35 * ("Controllers" in report["features"]) \
        + 20 * ("OpenAL" in report["features"]) + 15 * ("LWJGL2" in report["features"])
    return score

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("jars", nargs="+", type=Path)
    args = parser.parse_args()
    reports = []
    for path in args.jars:
        if not path.is_file():
            print(f"SKIP {path}: not found", file=sys.stderr)
            continue
        report = inspect(path)
        reports.append(report)
        print(f"\nCLIENT: {report['path']}")
        print(f"Classes: {report['classes']} | ZIP entries: {report['entries']}")
        print(f"Class versions (major.minor): {report['versions']}")
        print(f"Known entrypoints: {report['entrypoints'] or 'not detected'}")
        print(f"APIs: {', '.join(report['features']) or 'not detected'}")
        print(f"Bundled native libraries: {len(report['native'])}")
        print(f"Non-class resources: {report['resources']}")
        print(f"Android-first score (lower is better): {rank(report)}")
    if len(reports) > 1:
        best = min(reports, key=rank)
        print(f"\nFIRST CANDIDATE: {best['path']}")
        print("This is a static compatibility ranking, not proof that the client launches.")
    if not reports:
        raise SystemExit("No input JARs could be inspected.")

if __name__ == "__main__":
    main()
