# SolarCrystal 0.8.5 — Unified typed-frame execution

## Goal
Combine the primitive integer subset and object/reference subset in one VmFrame. A method can keep integer values and object references in typed locals and one operand stack, access integer instance fields, and return an integer.

## Added bytecodes
Integer constants (iconst_m1 through iconst_5, bipush), integer locals (iload, iload_0..3, istore, istore_0..3), arithmetic (iadd, isub, imul, idiv, ineg), reference/object operations (aload, astore, dup, pop, new, invokespecial <init>()V, getfield, putfield), and returns (ireturn, return).

## Validation
The native test runs a mixed method that reads an object's integer field, adds one, stores the result in a local and returns it. A second method validates integer-only arithmetic and locals. Constructor, field, null-reference, stack and instruction-budget tests remain.

## Boundaries
This is a bounded unified bytecode executor, not a complete JVM and not yet the production class-method path. Constant-pool resolution still uses callbacks rather than parsed class metadata. General method invocation, arrays, reference fields, inheritance dispatch, class initialization, exceptions, verification, floating point, long/double, broad JDK/LWJGL support are not implemented. The legacy execute_bytecode and class method entry point remain unchanged.
