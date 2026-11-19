# SolarCrystal 0.8.3b — same-class invokestatic design

## Goal

Execute a Java 8 class method that invokes another static method in the same class:

```java
public static int helper(int value) { return value * 3; }
public static int callsHelper(int value) { return helper(value) + 1; }
```

The acceptance test is `callsHelper(5) == 16`.

## Current limitation

The 0.7 native executor extracts only one requested method's Code attribute. Its constant-pool parser currently discards the two indices in CONSTANT_Methodref and CONSTANT_InterfaceMethodref entries. The bytecode interpreter has one operand stack and one locals array, and has no call-frame mechanism. Therefore adding opcode 0xb8 alone would be unsafe and insufficient.

## Required implementation

1. Parse the class once into a bounded class model:
   - UTF8 and Class constants;
   - NameAndType constants;
   - Methodref constants (owner, name, descriptor);
   - every method's access flags, descriptor, max_stack, max_locals and Code bytes.
2. Validate invocation targets:
   - only CONSTANT_Methodref (not interface dispatch);
   - owner must equal the currently loaded class;
   - target must be static and have a Code attribute;
   - descriptor arguments and return type must be supported integer types;
   - reject malformed constant-pool indices and unsupported descriptors.
3. Add interpreter call frames:
   - each frame owns its bytecode, program counter, locals, operand stack and max_stack;
   - invokestatic pops arguments in JVM order, creates a fresh callee frame, and transfers an integer return value to the caller;
   - enforce a bounded call depth and a shared instruction budget across all frames.
4. Keep current public native API behavior for existing single-method tests.
5. Add on-device regression checks for the existing arithmetic methods and `callsHelper(5) == 16`.

## Explicit non-goals

This milestone does not implement object allocation, instance methods, fields, virtual dispatch, arrays, exceptions, class initialization, JDK classes, or Minecraft compatibility. Those require separate milestones.

## Acceptance criteria

- Existing 0.8.2 integer tests remain passing.
- The fixture class loads and its metadata resolves.
- `helper(5)` returns 15.
- `callsHelper(5)` executes its invokestatic instruction and returns 16.
- Recursion/depth and instruction limits fail safely rather than overflowing native stack or running indefinitely.
- Android APK compiles and the on-device test reports PASS.
