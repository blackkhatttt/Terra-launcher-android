import com.universe.solarcrystal.RubyDungBlockWorld;
import com.universe.solarcrystal.RubyDungVoxelMesher;

public final class RubyDungVoxelMesherTest {
    public static void main(String[] args) {
        String result = RubyDungVoxelMesher.selfTest();
        if (!result.startsWith("PASS:")) throw new AssertionError(result);
        System.out.println(result);

        RubyDungBlockWorld world = new RubyDungBlockWorld()
            .withBlock(new RubyDungBlockWorld.Block(-2, 3, 4, "stone"));
        RubyDungVoxelMesher.Mesh mesh = RubyDungVoxelMesher.build(world);
        float[] positions = mesh.positions();
        positions[0] = 999.0f;
        if (mesh.positions()[0] == 999.0f)
            throw new AssertionError("Mesh arrays must be defensive copies");
        int[] indices = mesh.indices();
        for (int index : indices) {
            if (index < 0 || index >= mesh.vertexCount())
                throw new AssertionError("Mesh index out of range");
        }
        for (String material : mesh.faceMaterials()) {
            if (!"stone".equals(material))
                throw new AssertionError("Incorrect face material");
        }
        try {
            RubyDungVoxelMesher.build(null);
            throw new AssertionError("Null world must be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected validation.
        }
        System.out.println("PASS: defensive arrays, valid indices, material preservation and null validation.");
    }
}
