public final class SolarCrystalStaticFixture {
    public static final int MAGIC = 731;
    public static final long BIG_MAGIC = 0x1020304050607080L;
    public static final String LABEL = "SolarCrystal";
    public static int counter;
    public static int computed;
    static {
        counter = 4;
        computed = counter * 3;
    }
    public static int readCounter() { return counter; }
}
