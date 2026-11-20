# SolarCrystal 0.8.7c — Inherited methods and interfaces

## Goal
Resolve inherited instance methods through registered superclass metadata and locate interface instance methods, including supported Java 8 default methods.

## Implementation
- ClassMethodResolver walks the receiver's superclass chain before checking implemented interfaces.
- Interface lookup recursively follows superinterfaces and detects repeated nodes.
- Private and static methods are excluded from instance-method resolution.
- Interface default methods must be concrete and have executable Code.
- Ambiguous unrelated default methods are rejected instead of choosing arbitrarily.

## Validation
Java 8 fixtures verify an inherited superclass method, a default interface method, direct interface lookup, missing method behavior, and rejection when a class is supplied where an interface is required.

## Limits
This is metadata resolution only. It does not add invokevirtual/invokeinterface bytecode execution, dynamic dispatch from executing Java code, bridge-method selection, generic signatures, full JVM maximally-specific interface rules, or complete access-control verification.
