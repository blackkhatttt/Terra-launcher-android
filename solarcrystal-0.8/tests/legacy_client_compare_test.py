#!/usr/bin/env python3
import tempfile
import zipfile
from pathlib import Path
import importlib.util

script = Path(__file__).resolve().parents[2] / "tools" / "legacy_client_compare.py"
spec = importlib.util.spec_from_file_location("legacy_client_compare", script)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

with tempfile.TemporaryDirectory() as tmp:
    jar_path = Path(tmp) / "rubydung-fixture.jar"
    class_data = b"\xca\xfe\xba\xbe\x00\x00\x00\x32" + b"\x00" * 16 + b"org/lwjgl/opengl/GL11"
    with zipfile.ZipFile(jar_path, "w") as jar:
        jar.writestr("com/mojang/rubydung/RubyDung.class", class_data)
        jar.writestr("terrain.png", b"fixture")
    report = module.inspect(jar_path)
    assert report["classes"] == 1
    assert report["entrypoints"] == ["com/mojang/rubydung/RubyDung.class"]
    assert "OpenGL" in report["features"]
print("SolarCrystal legacy client comparison tests passed.")
