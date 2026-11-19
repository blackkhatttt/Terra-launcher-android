public final class SolarCrystalJarFixture {
    private SolarCrystalJarFixture() {}

    public static int multiply(int left, int right) { return left * right; }
    public static int add(int left, int right) { return left + right; }
    public static int remainder(int left, int right) { return left % right; }
    public static int bitwiseAnd(int left, int right) { return left & right; }
    public static int shiftLeft(int value, int amount) { return value << amount; }
    public static int absolute(int value) { return value < 0 ? -value : value; }
}