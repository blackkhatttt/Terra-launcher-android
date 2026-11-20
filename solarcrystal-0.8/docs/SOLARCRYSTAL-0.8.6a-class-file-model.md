# SolarCrystal 0.8.6a — Class-file model

## Goal
Create a bounded native representation of a Java .class file so later runtime
stages can resolve bytecode operands from the class's actual constant pool.

## Included
- Parse class identity, superclass, interfaces and access flags.
- Preserve typed constant-pool entries and their indices.
- Parse field names, descriptors and access flags.
- Parse method names, descriptors, access flags and Code byte arrays.
- Resolve CONSTANT_Class, CONSTANT_Fieldref, CONSTANT_Methodref and
  CONSTANT_InterfaceMethodref entries to owner/name/descriptor.
- Find a declared method by exact name and descriptor.
- Bounds checks for class bytes, code length, attribute lengths and CP indices.
- Native host test compiled with warnings treated as errors.

## Not included
This stage does not execute a parsed method, connect the model to the global
class registry, implement Java verification, support every JVM instruction,
resolve arbitrary inherited fields, or provide a JDK. Executor integration is
the next 0.8.6 substage.
