# SolarCrystal 0.8.7d — Multi-class member linkage

This stage adds a metadata-only resolver for parsed constant-pool field and method references across registered classes.

- Field references resolve through the declaring class and its superclass chain.
- Class method references resolve through the class method resolver.
- Interface method references resolve through interface metadata and default methods.
- Constructors are checked against the exact registered owner class.
- Native tests compile Java 8 fixtures and verify that a child class links references to a parent field, inherited method, and interface default method.

This does not execute general invokevirtual/invokeinterface instructions, define JVM classes, verify bytecode, initialize classes, or provide a complete Java runtime.
