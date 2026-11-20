public class SolarCrystalInheritedChildFixture extends SolarCrystalInheritedBaseFixture {
    public int readInherited() { return this.inheritedValue; }
    public void writeInherited(int next) { this.inheritedValue = next; }
}
