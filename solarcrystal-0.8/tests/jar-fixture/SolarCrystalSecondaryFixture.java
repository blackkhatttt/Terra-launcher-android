public class SolarCrystalSecondaryFixture {
    public int value;

    public SolarCrystalSecondaryFixture() {
        value = 19;
    }

    public int readPrimaryValue(SolarCrystalObjectFixture primary) {
        return primary.value;
    }

    public void writePrimaryValue(SolarCrystalObjectFixture primary, int next) {
        primary.value = next;
    }

    public SolarCrystalObjectFixture makePrimary() {
        return new SolarCrystalObjectFixture();
    }
}
