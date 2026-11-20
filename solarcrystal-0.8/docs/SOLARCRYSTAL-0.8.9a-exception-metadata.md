# SolarCrystal 0.8.9a — Java exception metadata

This milestone extends the class-file model to preserve and validate the exception table from each method's Code attribute.

Supported metadata:
- start_pc and end_pc protected bytecode range
- handler_pc exception handler entry point
- catch_type constant-pool class reference
- catch_type == 0 catch-all/finally handler
- resolved internal catch class name

Validation rejects invalid bytecode ranges, invalid catch-type constant-pool entries, truncated tables, and Code attribute length mismatches.

Scope:
- This milestone parses exception metadata only.
- The bytecode executor does not yet dispatch thrown exceptions to handlers.
- Runtime athrow, Java exception object semantics, stack traces, and finally execution are not implemented here.
- These are later runtime milestones; this is not a complete JVM exception subsystem.
