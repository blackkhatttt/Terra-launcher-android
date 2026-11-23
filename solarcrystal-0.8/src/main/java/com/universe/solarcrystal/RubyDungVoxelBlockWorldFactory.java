package com.universe.solarcrystal;

/** Builds a small deterministic terrain scene for validating the OpenGL voxel renderer. */
public final class RubyDungVoxelBlockWorldFactory {
    public RubyDungBlockWorld createTestWorld() {
        RubyDungBlockWorld world = new RubyDungBlockWorld();
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                int height = terrainHeight(x, z);
                for (int y = 0; y < height; y++) {
                    String material = y == height - 1 ? "grass"
                        : (y >= height - 2 ? "dirt" : "stone");
                    world = world.withBlock(
                        new RubyDungBlockWorld.Block(x, y, z, material));
                }
            }
        }
        return world;
    }

    private static int terrainHeight(int x, int z) {
        double waves = Math.sin(x * 0.38) * 0.55
            + Math.cos(z * 0.34) * 0.45
            + Math.sin((x + z) * 0.22) * 0.35;
        return 2 + Math.max(0, (int) Math.round(waves + 0.45));
    }

    public static String selfTest() {
        RubyDungBlockWorld world = new RubyDungVoxelBlockWorldFactory().createTestWorld();
        if (world.size() < 225) return "FAIL: generated test terrain is unexpectedly small";
        RubyDungVoxelMesher.Mesh mesh = RubyDungVoxelMesher.build(world);
        if (mesh.faceCount() <= 0 || mesh.indexCount() != mesh.faceCount() * 6)
            return "FAIL: generated terrain mesh is invalid";
        return "PASS: deterministic terrain fixture generated " + world.size()
            + " blocks and " + mesh.faceCount() + " exposed faces.";
    }
}
