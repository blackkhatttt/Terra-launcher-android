# SolarCrystal 0.8.8a — Arrays

Adds a bounded array heap and a dedicated array-bytecode executor.

Supported operations:
- `newarray` for int arrays (`atype=10`)
- `anewarray` for reference arrays using a constant-pool class resolver
- `iaload`, `iastore`, `aaload`, `aastore`, and `arraylength`
- Null-initialized reference arrays, index/type validation, negative-size rejection, and allocation/instruction limits

The array heap uses tagged object identifiers in a reserved high-bit range so array handles do not collide with the existing object heap IDs.

This stage is an isolated bounded executor; it does not yet connect array operations to the parsed-method execution pipeline. It does not implement multidimensional arrays, byte/char/short/boolean/long/float/double arrays, covariance/store checks against loaded classes, or the full JVM array model.
