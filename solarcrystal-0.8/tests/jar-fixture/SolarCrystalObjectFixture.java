public final class SolarCrystalObjectFixture {
    public int value;
    public SolarCrystalObjectFixture() { this.value = 42; }
    public void writeValue(int next) { this.value = next; }
    public int readValue() { return this.value; }
}