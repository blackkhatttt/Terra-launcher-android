# SolarCrystal 0.8.4d — constructor execution foundation

## Implemented

- A bounded `invokespecial` path for no-argument `<init>()V` calls.
- Constructor bodies execute in a nested `VmFrame`, with local 0 containing the receiver.
- `java/lang/Object.<init>()V` is treated as an intrinsic no-op.
- Same-class integer instance fields can be initialized through `putfield` and read through `getfield`.
- Added `ObjectHeap::class_name` and dynamic integer-field definition.
- The shared instruction budget applies across nested constructor calls; constructor recursion is capped.

## Supported constructor bytecodes

`aload`, `aload_0..3`, `astore`, `astore_0..3`, `new`, `dup`, `pop`, `invokespecial`, `putfield`, `getfield`, `bipush`, `iconst_m1..5`, `iadd`, and constructor `return`.

The caller provides resolvers for class, method, and field constant-pool references. The executor validates the resolved method name and descriptor.

## Limitations

This is not general JVM method dispatch. Only no-argument void constructors are supported. Only same-class constructors and the intrinsic `java/lang/Object` constructor are supported; arbitrary superclass constructor chains, constructor arguments, reference fields, other primitive field types, access checks, class initialization, verification, exceptions, and general `invokespecial` calls remain unsupported. The class-file parser is not yet wired to these resolver callbacks.

## Tests

Native tests cover object creation, constructor dispatch, local receiver setup, integer field initialization/readback, intrinsic Object constructor handling, invalid owner rejection, and instruction-budget enforcement.
