# SolarCrystal 0.8.7b — Inherited integer fields

## Goal
Resolve integer instance-field references through a registered superclass chain so bytecode compiled in a child class can read and write an integer field declared by its parent.

## Changes
- The class repository exposes registered class pointers.
- The execution resolver walks superclass metadata to locate the declaring field.
- Field resolution records the bytecode access class and known registered runtime classes assignable to it.
- getfield and putfield validate the receiver against that compatibility set.
- Existing integer heap storage remains in use; field hiding is not represented as distinct JVM field identities.

## Validation
Java fixtures compile to Java 8 bytecode. The native test parses the child class file, resolves a Fieldref owned by the child to a field declared by its parent, writes 73 through child bytecode, and reads 73 back.

## Limits
Only int instance fields and registered superclass chains are supported. Interfaces, field hiding, reference/static fields, constructor chaining, general method dispatch, class initialization, verification, exceptions, arrays, and full JVM behavior are not implemented.
