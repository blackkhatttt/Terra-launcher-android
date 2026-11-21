#!/usr/bin/env python3
"""Offline JAR/class preflight checks for the SolarCrystal Infdev preparation track."""
from pathlib import Path
from zipfile import ZipFile, BadZipFile
import sys
import time

if len(sys.argv) != 2:
    raise SystemExit("Usage: infdev_preflight.py <fixture.jar>")
jar_path = Path(sys.argv[1])
if not jar_path.is_file():
    raise SystemExit(f"JAR not found: {jar_path}")

started = time.perf_counter()
try:
    with ZipFile(jar_path) as jar:
        bad = jar.testzip()
        if bad:
            raise SystemExit(f"JAR CRC check failed at: {bad}")
        entries = [i for i in jar.infolist() if not i.is_dir()]
        names = [i.filename for i in entries]
        if len(names) != len(set(names)):
            raise SystemExit("JAR contains duplicate entry names")
        classes = [i for i in entries if i.filename.endswith(".class")]
        if not classes:
            raise SystemExit("JAR contains no Java class files")
        versions = []
        total_class_bytes = 0
        for info in classes:
            data = jar.read(info)
            if len(data) < 8 or data[:4] != b"\\xca\\xfe\\xba\\xbe":
                raise SystemExit(f"Invalid class-file header: {info.filename}")
            minor = int.from_bytes(data[4:6], "big")
            major = int.from_bytes(data[6:8], "big")
            if major < 45 or major > 65:
                raise SystemExit(f"Unsupported class-file major version {major}: {info.filename}")
            versions.append((major, minor))
            total_class_bytes += len(data)
except BadZipFile as exc:
    raise SystemExit(f"Invalid JAR/ZIP archive: {exc}")

elapsed_ms = (time.perf_counter() - started) * 1000.0
print("SOLARCRYSTAL INFDEV PREFLIGHT: PASS")
print(f"Archive: {jar_path.name}")
print(f"Entries: {len(entries)} | Classes: {len(classes)}")
print(f"Class bytes: {total_class_bytes} | Class versions: {sorted(set(versions))}")
print(f"Archive scan: {elapsed_ms:.3f} ms")
