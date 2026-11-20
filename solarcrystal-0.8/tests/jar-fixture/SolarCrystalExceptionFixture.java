public final class SolarCrystalExceptionFixture {
    public static int safeDivide(int left, int right) {
        try { return left / right; }
        catch (ArithmeticException exception) { return -1; }
    }
    public static int withFinally(int value) {
        try { return value + 1; }
        finally { value = value + 2; }
    }
}
