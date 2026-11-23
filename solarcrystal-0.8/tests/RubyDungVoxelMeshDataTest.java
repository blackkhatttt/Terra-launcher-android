import com.universe.solarcrystal.RubyDungBlockWorld;
import com.universe.solarcrystal.RubyDungVoxelMeshData;
import com.universe.solarcrystal.RubyDungVoxelMesher;

public final class RubyDungVoxelMeshDataTest {
    public static void main(String[] args) {
        String result = RubyDungVoxelMeshData.selfTest();
        if (!result.startsWith("PASS:")) throw new AssertionError(result);
        System.out.println(result);

        RubyDungBlockWorld world = new RubyDungBlockWorld()
            .withBlock(new RubyDungBlockWorld.Block(0, 0, 0, "stone"))
            .withBlock(new RubyDungBlockWorld.Block(1, 0, 0, "grass"));
        RubyDungVoxelMeshData data =
            RubyDungVoxelMeshData.from(RubyDungVoxelMesher.build(world));
        if (data.vertexCount() != 40 || data.indexCount() != 60)
            throw new AssertionError("Adjacent block GPU mesh counts are incorrect");

        float[] positions = data.positions();
        positions[0] = 999.0f;
        if (data.positions()[0] == 999.0f)
            throw new AssertionError("Position arrays must be defensive copies");
        float[] colors = data.colors();
        colors[0] = 999.0f;
        if (data.colors()[0] == 999.0f)
            throw new AssertionError("Color arrays must be defensive copies");

        try {
            RubyDungVoxelMeshData.from(null);
            throw new AssertionError("Null mesh must be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected validation.
        }
        System.out.println("PASS: adjacent blocks, index conversion, defensive arrays and null validation.");
    }
}
