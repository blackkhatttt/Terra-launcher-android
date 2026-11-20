public class SolarCrystalDependencyFixture extends SolarCrystalSecondaryFixture
        implements SolarCrystalMarkerFixture {
    public SolarCrystalMissingFixture missing;
    public SolarCrystalSecondaryFixture linked;

    @Override
    public int markerValue() {
        return linked == null ? 0 : linked.sum(20, 22);
    }

    public SolarCrystalMissingFixture missingValue(
            SolarCrystalMissingFixture input) {
        return input;
    }
}
