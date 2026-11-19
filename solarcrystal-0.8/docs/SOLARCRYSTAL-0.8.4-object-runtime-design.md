# SolarCrystal 0.8.4 — object runtime design

## Goal

Introduce a deliberately small, bounded object model that can execute a Java 8 fixture which allocates an object, initializes one integer field, reads it back, and returns the result.

Acceptance fixture (conceptual):

```java
final class SolarCrystalBox {
    int value;
    SolarCrystalBox(int value) { this.value = value; }
    static int createAndRead(int value) {
        SolarCrystalBox box = new SolarCrystalBox(value);
        return box.value;
    }
}
```

The target result is `createAndRead(37) == 37`.

## Why this is a separate runtime milestone

The current integer interpreter represents every operand-stack value and local as int32. Java object references cannot safely be represented as ordinary integers without a distinct value type and a managed heap. The class parser must also retain Fieldref and constructor/method references. Adding opcode cases alone would permit type confusion and invalid memory access.

Before implementing object opcodes, the runtime must establish:

1. A tagged VM value: at minimum Integer and Reference (with a null reference representation).
2. A VM-owned heap with stable object IDs, class identity, and field storage. Never expose native pointers as Java references.
3. A per-execution context shared by nested method frames: heap, call-depth counter, and one instruction budget.
4. A structured class model retaining Class, NameAndType, Fieldref, Methodref, method access flags, descriptors, Code, and field descriptors/access flags.
5. Descriptor validation and field-layout metadata. Initial scope is instance fields of type int only.
6. Per-frame locals and operand stacks containing tagged VM values; enforce max_stack and max_locals.
7. Explicit checks for null, invalid references, wrong owner, wrong field descriptor, static-vs-instance misuse, and unsupported opcodes.

## Planned bytecodes

Implement in dependency order, with tests after each group:

- Reference locals: `aload`, `aload_0` through `aload_3`, `astore`, `astore_0` through `astore_3`.
- Stack: `dup`, `pop`.
- Allocation: `new` for classes in the current parsed class only.
- Constructor call: `invokespecial` restricted initially to the same class's `<init>` method and int arguments.
- Fields: `getfield` and `putfield` for same-class int instance fields.
- Existing integer opcodes continue to operate only on Integer values.
- Preserve same-class static integer calls where the current runtime can do so reliably.

## Safety and bounds

- Maximum class bytes: 16 MiB.
- Maximum heap objects per execution: 4096.
- Maximum fields per object: bounded by parsed class metadata.
- Maximum call depth: 32.
- Shared instruction budget: 1,000,000 across all frames.
- Validate every constant-pool index and bytecode operand before use.
- Fail closed on unsupported descriptors, external classes, arrays, interfaces, exceptions, and unsupported attributes/opcodes.

## Test plan

1. Integer-only regression suite remains unchanged and passes.
2. Construct an object and initialize an int field through its constructor.
3. Read the field and return the original value.
4. Mutate the field and verify the updated value.
5. Verify null, invalid field reference, wrong owner/type, and heap/depth/budget limits fail safely.
6. Run the full Android APK build and the on-device regression suite.

## Non-goals

This milestone does not provide general JVM object compatibility, inheritance dispatch, arrays, strings, reflection, class initialization, garbage collection, JDK classes, LWJGL, or Minecraft compatibility. It is a first controlled object subset only.
