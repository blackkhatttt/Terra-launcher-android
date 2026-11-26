import com.universe.solarcrystal.RubyDungBlockWorld;
import com.universe.solarcrystal.RubyDungVoxelPlayerController;

public final class RubyDungVoxelPlayerControllerTest {
    public static void main(String[] args) {
        String result = RubyDungVoxelPlayerController.selfTest();
        if (!result.startsWith("PASS:")) throw new AssertionError(result);
        System.out.println(result);
        RubyDungBlockWorld world = new RubyDungBlockWorld()
            .withBlock(new RubyDungBlockWorld.Block(0, 0, 0, "stone"));
        try {
            new RubyDungVoxelPlayerController(world, 0.5, 0.5, 0.5);
            throw new AssertionError("Spawn inside a block must be rejected");
        } catch (IllegalArgumentException expected) {
            System.out.println("PASS: invalid spawn collision is rejected.");
        }
    }
}
