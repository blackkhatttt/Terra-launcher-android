import com.universe.solarcrystal.RubyDungPickingScene;
import com.universe.solarcrystal.RubyDungRayPicker;

public final class RubyDungPickingSceneTest {
    public static void main(String[] args) {
        String result = RubyDungPickingScene.selfTest();
        if (!result.startsWith("PASS:")) throw new AssertionError(result);
        System.out.println(result);

        RubyDungPickingScene empty = new RubyDungPickingScene();
        if (empty.pick(new RubyDungRayPicker.Vec3(0, 0, 0),
                new RubyDungRayPicker.Vec3(1, 0, 0), 10) != null)
            throw new AssertionError("Empty scene must not select anything");

        RubyDungPickingScene scene = empty.withTarget(new RubyDungPickingScene.Target(
            "wall", new RubyDungRayPicker.Aabb(
                new RubyDungRayPicker.Vec3(2, -1, -1),
                new RubyDungRayPicker.Vec3(3, 1, 1))));
        try {
            scene.withTarget(new RubyDungPickingScene.Target("wall",
                new RubyDungRayPicker.Aabb(new RubyDungRayPicker.Vec3(4, 0, 0),
                    new RubyDungRayPicker.Vec3(5, 1, 1))));
            throw new AssertionError("Duplicate target id should be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected validation.
        }
        if (scene.withoutTarget("missing") != scene)
            throw new AssertionError("Removing an unknown target should preserve the scene");
        System.out.println("PASS: empty scene, duplicate IDs and no-op removal.");
    }
}
