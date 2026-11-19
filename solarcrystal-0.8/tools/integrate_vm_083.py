#!/usr/bin/env python3
from pathlib import Path
import shutil

root = Path.cwd()
base = Path(__file__).resolve().parent
cpp = root / "terra-android/src/main/cpp"
native = base.parent / "native"

header = cpp / "include/vm_runtime.h"
text = header.read_text(encoding="utf-8")
if "using StaticCallHandler" not in text:
    text = text.replace("#include <cstdint>", "#include <cstdint>\n#include <functional>")
    text = text.replace("namespace solarcrystal {", "namespace solarcrystal {\n\nusing StaticCallHandler = std::function<std::int32_t(std::uint16_t, std::vector<std::int32_t>&)>;")
    text = text.replace("    const std::vector<std::int32_t>& initial_locals = {}\n", "    const std::vector<std::int32_t>& initial_locals = {},\n    const StaticCallHandler& static_call = {},\n    std::size_t* shared_steps = nullptr\n")
    header.write_text(text, encoding="utf-8")

vm = cpp / "vm_runtime.cpp"
text = vm.read_text(encoding="utf-8")
if "call_handler(static_call)" not in text:
    text = text.replace("        const std::vector<std::int32_t>& initial_locals\n    )", "        const std::vector<std::int32_t>& initial_locals,\n        const StaticCallHandler& static_call,\n        std::size_t* shared_steps\n    )")
    text = text.replace("          locals(local_count, 0) {", "          locals(local_count, 0),\n          call_handler(static_call),\n          shared_instruction_steps(shared_steps) {")
    text = text.replace('            if (++steps > 1000000)\n                throw std::runtime_error("Execution step limit exceeded");', '            if (shared_instruction_steps) {\n                if (++(*shared_instruction_steps) > 1000000)\n                    throw std::runtime_error("Shared execution step limit exceeded");\n                ++steps;\n            } else if (++steps > 1000000) {\n                throw std::runtime_error("Execution step limit exceeded");\n            }')
    text = text.replace("                case 0x02:\n", '                case 0xb8: {\n                    const std::uint16_t reference = read_u2();\n                    if (!call_handler) throw std::runtime_error("invokestatic has no class method resolver");\n                    push(call_handler(reference, stack));\n                    break;\n                }\n\n                case 0x02:\n', 1)
    text = text.replace("    std::vector<std::int32_t> stack;\n", "    std::vector<std::int32_t> stack;\n    StaticCallHandler call_handler;\n    std::size_t* shared_instruction_steps;\n")
    text = text.replace("    const std::vector<std::int32_t>& initial_locals\n) {", "    const std::vector<std::int32_t>& initial_locals,\n    const StaticCallHandler& static_call,\n    std::size_t* shared_steps\n) {")
    text = text.replace("            initial_locals\n        );", "            initial_locals,\n            static_call,\n            shared_steps\n        );")
    vm.write_text(text, encoding="utf-8")

# Keep the 0.8.2 arithmetic extension, which runs immediately before this script.
text = vm.read_text(encoding="utf-8")
if "case 0x70:" not in text:
    raise SystemExit("0.8.2 integer opcode integration missing; refusing to overwrite VM")
shutil.copy2(native / "invokestatic_executor.cpp", cpp / "invokestatic_executor.cpp")

legacy = cpp / "class_method_executor.cpp"
text = legacy.read_text(encoding="utf-8")
if "execute_class_method_legacy(" not in text:
    needle = "std::string execute_class_method("
    if text.count(needle) != 1:
        raise SystemExit("Could not uniquely locate legacy class method executor")
    text = text.replace(needle, "std::string execute_class_method_legacy(", 1)
    legacy.write_text(text, encoding="utf-8")

cmake = cpp / "CMakeLists.txt"
text = cmake.read_text(encoding="utf-8")
if "invokestatic_executor.cpp" not in text:
    text = text.replace("    class_method_executor.cpp\n", "    class_method_executor.cpp\n    invokestatic_executor.cpp\n")
    cmake.write_text(text, encoding="utf-8")

print("SolarCrystal 0.8.3b invokestatic runtime integrated.")
