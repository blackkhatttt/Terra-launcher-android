# SolarCrystal 0.8.6b — Constant Pool execution resolver

## Goal

Connect parsed class-file Constant Pool entries to the bounded reference bytecode executor. The executor no longer needs test callbacks with manually invented class/member values for the supported operations.

## Implementation

- `ClassFileExecutionResolver` adapts a parsed `ClassFileModel` into the executor's existing resolver callbacks.
- `new` resolves `CONSTANT_Class` entries.
- `invokespecial` resolves `CONSTANT_Methodref` / `CONSTANT_InterfaceMethodref` constructor references, including the intrinsic `java/lang/Object.<init>()V`.
- `getfield` and `putfield` resolve `CONSTANT_Fieldref` entries and validate the declared field.
- The native test compiles and parses a real Java 8 `.class`, executes its constructor, then executes the actual `writeValue(I)V` and `readValue()I` Code attributes. The expected field value is 73.

## Deliberate limits

This is a single-loaded-class adapter, not a class loader or classpath resolver. It supports no-argument constructors and integer instance fields only. It does not add `invokevirtual`, general method dispatch, superclass constructor chains, static fields, arrays, reference fields, class initialization, exception handling, bytecode verification, floating-point operations, or a complete JVM/JDK. It does not make Minecraft runnable yet.

## Validation

The workflow compiles native code with C++17, `-Wall -Wextra -Werror`, builds the Java fixture with `javac --release 8 -g:none`, runs the native integration test, and builds the Android debug APK.
