# SolarCrystal 0.8.4c — reference bytecode foundation

## Implemented scope

This increment adds a bounded reference-bytecode executor alongside the existing integer-only class-method interpreter.

Supported opcodes:
- `aload` and `aload_0` through `aload_3`
- `astore` and `astore_0` through `astore_3`
- `dup`
- `pop`
- `new`, using a caller-provided constant-pool class resolver

The executor uses `VmFrame`, tagged `VmValue` values, and `ObjectHeap`. Object references are heap IDs, never native pointers. `new` allocates an object with an empty field layout; field metadata and constructors are not yet executed.

## Explicit limitations

This is a first reference-opcode execution path, not yet wired into the existing `execute_bytecode` integer interpreter or Java class-method executor. It does not yet parse class-file Fieldref/Methodref metadata, execute `invokespecial`, initialize fields, or run arbitrary Java constructors. The class resolver is supplied by the caller, and unsupported opcodes fail closed.

## Tests

The native test checks object allocation, reference duplication, local reference stores/loads, null handling, invalid type rejection, truncated operands, unsupported opcodes, and instruction-budget enforcement.

Next: connect parsed class metadata and a shared execution context, then implement same-class `invokespecial <init>` and integer instance fields.
