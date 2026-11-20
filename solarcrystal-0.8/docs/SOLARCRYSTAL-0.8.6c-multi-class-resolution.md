# SolarCrystal 0.8.6c — Multi-class class repository

## Goal

Resolve CONSTANT_Class, constructor Methodref, and integer instance-field Fieldref entries against more than one parsed class file.

## Implementation

- ClassFileRepository registers non-owning references to parsed ClassFileModel objects by internal JVM class name.
- Duplicate registrations and empty class names are rejected.
- ClassFileExecutionResolver can use the repository to resolve class, constructor, and field owners declared in another registered class.
- The single-class constructor remains available and only resolves its own class plus the intrinsic java/lang/Object constructor.
- Cross-class integer field access is accepted only when the runtime object has the same class as the declaring field owner. Inheritance and superclass field layouts are not implemented in this stage.
- The repository does not own class models; registered models must outlive it and any resolvers using it.

## Verification

The native test compiles two real Java 8 class files. A method parsed from the secondary class resolves the primary class constructor and field, then executes getfield and putfield against a primary-class object. It also checks missing classes and duplicate registration.

## Limits

This is multi-class metadata and member-reference resolution, not general JVM linking. It does not add invokevirtual, invokestatic dispatch across loaded classes, inheritance, reference fields, arrays, class initialization, exceptions, verification, or JDK/LWJGL support.
