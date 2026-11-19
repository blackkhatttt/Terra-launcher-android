#!/usr/bin/env python3
from pathlib import Path

path = Path("terra-android/src/main/cpp/vm_runtime.cpp")
source = path.read_text(encoding="utf-8")

anchor = """                case 0x6c: {
                    const auto b = pop();
                    const auto a = pop();

                    if (b == 0)
                        throw std::runtime_error(
                            "ArithmeticException: division by zero"
                        );

                    if (a == std::numeric_limits<std::int32_t>::min()
                        && b == -1) {
                        push(a);
                    } else {
                        push(a / b);
                    }
                    break;
                }
"""
extra = anchor + """
                case 0x70: {
                    const auto b = pop();
                    const auto a = pop();
                    if (b == 0)
                        throw std::runtime_error("ArithmeticException: remainder by zero");
                    push((a == std::numeric_limits<std::int32_t>::min() && b == -1)
                        ? 0 : a % b);
                    break;
                }

                case 0x78: {
                    const auto shift = static_cast<std::uint32_t>(pop()) & 31u;
                    const auto value = static_cast<std::uint32_t>(pop());
                    push(static_cast<std::int32_t>(value << shift));
                    break;
                }

                case 0x7a: {
                    const auto shift = static_cast<std::uint32_t>(pop()) & 31u;
                    push(pop() >> shift);
                    break;
                }

                case 0x7c: {
                    const auto shift = static_cast<std::uint32_t>(pop()) & 31u;
                    const auto value = static_cast<std::uint32_t>(pop());
                    push(static_cast<std::int32_t>(value >> shift));
                    break;
                }

                case 0x7e: {
                    const auto b = pop(); const auto a = pop();
                    push(a & b); break;
                }
                case 0x80: {
                    const auto b = pop(); const auto a = pop();
                    push(a | b); break;
                }
                case 0x82: {
                    const auto b = pop(); const auto a = pop();
                    push(a ^ b); break;
                }
"""
if "case 0x70:" not in source:
    if anchor not in source:
        raise SystemExit("Could not find integer division opcode block")
    source = source.replace(anchor, extra, 1)

anchor2 = """                case 0x57:
                    (void)pop();
                    break;
"""
extra2 = anchor2 + """
                case 0x5f: {
                    const auto first = pop();
                    const auto second = pop();
                    push(first);
                    push(second);
                    break;
                }
"""
if "case 0x5f:" not in source:
    if anchor2 not in source:
        raise SystemExit("Could not find pop opcode block")
    source = source.replace(anchor2, extra2, 1)

path.write_text(source, encoding="utf-8")
