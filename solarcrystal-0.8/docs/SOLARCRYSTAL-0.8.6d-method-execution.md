# SolarCrystal 0.8.6d — Execute parsed class methods

## Goal

Execute a selected method directly from its parsed ClassFileModel Code attribute.

## Implementation

- Locate methods by exact name and JVM descriptor.
- Build a typed VM frame from instance receiver and method arguments.
- Validate static versus instance invocation, argument count, integer/reference argument types, receiver class, and max_locals.
- Parse the supported descriptor subset: integer parameters, registered object-reference parameters, and integer or void return types.
- Resolve the executing method's Constant Pool through its own ClassFileExecutionResolver.
- Return the bounded reference executor's result and validate the method's declared return type.

## Test coverage

The CI test compiles real Java 8 fixture classes and executes a no-argument instance constructor, an instance method reading an integer field from another class, an instance method writing an integer field in another class, and a static integer addition method. It also checks missing methods and incorrect argument counts.

## Limits

This is direct method entry, not general bytecode invocation. It does not implement invokevirtual, invokestatic bytecode dispatch, inheritance, arrays, reference returns, strings, exceptions, class initialization, or full JVM verification. Supported parameter types are I and registered L...; references; supported returns are I and V.
