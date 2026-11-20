public class SolarCrystalMethodChildFixture extends SolarCrystalMethodBaseFixture
        implements SolarCrystalDefaultInterfaceFixture {
    public int readInherited() { return sharedValue; }
    public int callInherited() { return inheritedMethod(); }
    public int callInterfaceDefault() { return interfaceDefaultMethod(); }
}