public class SolarCrystalDependencyFixture extends SolarCrystalObjectFixture
        implements SolarCrystalMarkerFixture {
    public SolarCrystalMissingFixture missing;
    public SolarCrystalObjectFixture linked;

    @Override
    public int markerValue() {
        return linked == null ? 0 : linked.readValue();
    }

    public SolarCrystalMissingFixture missingValue(
            SolarCrystalMissingFixture input) {
        return input;
    }
}
