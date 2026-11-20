# SolarCrystal 0.8.7a — Class registry dependency analysis

## Goal

Use the parsed class repository to identify which class dependencies are already registered, which belong to known platform namespaces, and which remain unresolved.

## Implementation

- Adds a dependency analyzer over the existing ClassFileModel and ClassFileRepository.
- Collects dependencies from the superclass, interfaces, field descriptors, method descriptors, and CONSTANT_Class entries.
- Handles object types embedded in array descriptors.
- Deduplicates and sorts dependency names for deterministic reports.
- Separates registered classes, known platform namespaces (java, javax, jdk, sun, android), and unresolved names.
- Rejects analysis when the target class is not the exact model registered in the repository.
- Integrates the analyzer into the Android CMake source list.

## Test coverage

CI compiles real Java 8 fixtures and verifies a registered superclass, registered interface, platform Object superclass dependency, and an intentionally unregistered application class referenced by fields and method descriptors.

## Limits

This stage reports dependencies; it does not load missing classes, download libraries, initialize classes, perform inheritance checks, or resolve method dispatch. The platform namespace classification is only a namespace hint, not proof that every listed API exists on Android or the device.
