package com.universe.solarcrystal;

/**
 * Narrow host-side entry points for core Java operations.
 * This is a compatibility bridge foundation, not a complete JVM library.
 */
public final class JavaLibraryBridge {
    private JavaLibraryBridge() {}

    public static int absInt(int value) { return Math.abs(value); }
    public static long absLong(long value) { return Math.abs(value); }
    public static float absFloat(float value) { return Math.abs(value); }
    public static double absDouble(double value) { return Math.abs(value); }
    public static double sqrt(double value) { return Math.sqrt(value); }
    public static double sin(double value) { return Math.sin(value); }
    public static double cos(double value) { return Math.cos(value); }
    public static double floor(double value) { return Math.floor(value); }
    public static double ceil(double value) { return Math.ceil(value); }
    public static long currentTimeMillis() { return System.currentTimeMillis(); }
    public static long nanoTime() { return System.nanoTime(); }
    public static int compareInt(int a, int b) { return Integer.compare(a, b); }
    public static int compareLong(long a, long b) { return Long.compare(a, b); }
    public static String valueOfInt(int value) { return String.valueOf(value); }
    public static String valueOfLong(long value) { return String.valueOf(value); }
    public static String valueOfDouble(double value) { return String.valueOf(value); }
    public static StringBuilder newStringBuilder() { return new StringBuilder(); }
    public static int arrayCopy(Object source, int sourcePos, Object target,
                                int targetPos, int length) {
        System.arraycopy(source, sourcePos, target, targetPos, length);
        return length;
    }
    public static boolean isFinite(double value) { return Double.isFinite(value); }
}
