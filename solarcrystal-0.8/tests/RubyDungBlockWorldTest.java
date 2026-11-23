import com.universe.solarcrystal.RubyDungBlockWorld;
import com.universe.solarcrystal.RubyDungRayPicker;

public final class RubyDungBlockWorldTest {
    public static void main(String[] args) {
        String result = RubyDungBlockWorld.selfTest();
        if (!result.startsWith("PASS:")) throw new AssertionError(result);
        System.out.println(result);

        RubyDungBlockWorld empty = new RubyDungBlockWorld();
        if (empty.getBlock(0, 0, 0) != null)
            throw new AssertionError("Empty world must not contain blocks");

        RubyDungBlockWorld world = empty.withBlock(
            new RubyDungBlockWorld.Block(0, 0, 0, "stone"));
        try {
            world.withBlock(new RubyDungBlockWorld.Block(0, 0, 0, "dirt"));
            throw new AssertionError("Duplicate voxel coordinates must be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected validation.
        }
        if (world.withoutBlock(9, 9, 9) != world)
            throw new AssertionError("Removing an absent block must preserve the world");
        if (world.pick(new RubyDungRayPicker.Vec3(0.5, 0.5, -2),
                new RubyDungRayPicker.Vec3(0, 0, 1), 1) != null)
            throw new AssertionError("Maximum pick distance must be respected");
        System.out.println("PASS: empty world, duplicate voxel rejection, no-op removal and distance limit.");
    }
}
