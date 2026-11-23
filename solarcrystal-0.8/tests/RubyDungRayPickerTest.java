import com.universe.solarcrystal.RubyDungRayPicker;

public final class RubyDungRayPickerTest {
    public static void main(String[] args) {
        String result = RubyDungRayPicker.selfTest();
        if (!result.startsWith("PASS:")) {
            System.err.println(result);
            System.exit(1);
        }
        System.out.println(result);

        RubyDungRayPicker.Vec3 o = new RubyDungRayPicker.Vec3(0, 0, 0);
        RubyDungRayPicker.Vec3 d = new RubyDungRayPicker.Vec3(0, 0, 1);
        RubyDungRayPicker.Aabb box = new RubyDungRayPicker.Aabb(
            new RubyDungRayPicker.Vec3(-1, -1, 5),
            new RubyDungRayPicker.Vec3(1, 1, 6));
        RubyDungRayPicker.Hit hit = RubyDungRayPicker.intersect(o, d, box, 42, 10);
        if (hit == null || hit.objectIndex != 42 || Math.abs(hit.distance - 5.0) > 1e-9)
            throw new AssertionError("Z-axis ray intersection failed");
        if (RubyDungRayPicker.intersect(o, d, box, 0, 4.99) != null)
            throw new AssertionError("Maximum distance clipping failed");
        System.out.println("PASS: Z-axis intersection and maximum-distance clipping.");
    }
}
