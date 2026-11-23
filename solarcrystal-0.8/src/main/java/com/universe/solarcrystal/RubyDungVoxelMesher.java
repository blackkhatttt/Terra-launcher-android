package com.universe.solarcrystal;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds CPU-side triangle geometry for the exposed faces of a voxel world.
 * This is geometry data only; it does not upload buffers or call OpenGL.
 */
public final class RubyDungVoxelMesher {
    private RubyDungVoxelMesher() {}

    private static final int[][] NEIGHBORS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0},
        {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    // Four corners per face, ordered counter-clockwise when viewed from outside.
    private static final float[][][] CORNERS = {
        {{1,0,0},{1,1,0},{1,1,1},{1,0,1}},
        {{0,0,1},{0,1,1},{0,1,0},{0,0,0}},
        {{0,1,0},{0,1,1},{1,1,1},{1,1,0}},
        {{0,0,1},{0,0,0},{1,0,0},{1,0,1}},
        {{1,0,1},{1,1,1},{0,1,1},{0,0,1}},
        {{0,0,0},{0,1,0},{1,1,0},{1,0,0}}
    };

    public static final class Mesh {
        private final float[] positions;
        private final float[] normals;
        private final float[] textureCoordinates;
        private final int[] indices;
        private final String[] faceMaterials;

        private Mesh(List<Float> positions, List<Float> normals,
                     List<Float> textureCoordinates, List<Integer> indices,
                     List<String> faceMaterials) {
            this.positions = floats(positions);
            this.normals = floats(normals);
            this.textureCoordinates = floats(textureCoordinates);
            this.indices = ints(indices);
            this.faceMaterials = faceMaterials.toArray(new String[faceMaterials.size()]);
        }

        public int vertexCount() { return positions.length / 3; }
        public int faceCount() { return faceMaterials.length; }
        public int indexCount() { return indices.length; }
        public float[] positions() { return positions.clone(); }
        public float[] normals() { return normals.clone(); }
        public float[] textureCoordinates() { return textureCoordinates.clone(); }
        public int[] indices() { return indices.clone(); }
        public String[] faceMaterials() { return faceMaterials.clone(); }

        private static float[] floats(List<Float> values) {
            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) result[i] = values.get(i);
            return result;
        }

        private static int[] ints(List<Integer> values) {
            int[] result = new int[values.size()];
            for (int i = 0; i < values.size(); i++) result[i] = values.get(i);
            return result;
        }
    }

    public static Mesh build(RubyDungBlockWorld world) {
        if (world == null) throw new IllegalArgumentException("World must not be null");
        List<Float> positions = new ArrayList<Float>();
        List<Float> normals = new ArrayList<Float>();
        List<Float> uvs = new ArrayList<Float>();
        List<Integer> indices = new ArrayList<Integer>();
        List<String> materials = new ArrayList<String>();

        for (RubyDungBlockWorld.Block block : world.blocks()) {
            for (int face = 0; face < NEIGHBORS.length; face++) {
                int[] neighbor = NEIGHBORS[face];
                if (world.getBlock(block.x + neighbor[0],
                        block.y + neighbor[1], block.z + neighbor[2]) != null) {
                    continue;
                }

                int base = positions.size() / 3;
                for (int corner = 0; corner < 4; corner++) {
                    float[] point = CORNERS[face][corner];
                    positions.add(block.x + point[0]);
                    positions.add(block.y + point[1]);
                    positions.add(block.z + point[2]);
                    normals.add((float) neighbor[0]);
                    normals.add((float) neighbor[1]);
                    normals.add((float) neighbor[2]);
                }
                // Consistent 0-1 UVs for each exposed quad.
                uvs.add(0.0f); uvs.add(0.0f);
                uvs.add(0.0f); uvs.add(1.0f);
                uvs.add(1.0f); uvs.add(1.0f);
                uvs.add(1.0f); uvs.add(0.0f);

                indices.add(base); indices.add(base + 1); indices.add(base + 2);
                indices.add(base); indices.add(base + 2); indices.add(base + 3);
                materials.add(block.material);
            }
        }
        return new Mesh(positions, normals, uvs, indices, materials);
    }

    public static String selfTest() {
        RubyDungBlockWorld one = new RubyDungBlockWorld()
            .withBlock(new RubyDungBlockWorld.Block(0, 0, 0, "stone"));
        Mesh single = build(one);
        if (single.faceCount() != 6 || single.vertexCount() != 24
                || single.indexCount() != 36)
            return "FAIL: single block must produce six exposed quads";
        if (single.normals().length != 72 || single.textureCoordinates().length != 48)
            return "FAIL: vertex attributes have inconsistent lengths";
        RubyDungBlockWorld pair = one.withBlock(
            new RubyDungBlockWorld.Block(1, 0, 0, "grass"));
        Mesh joined = build(pair);
        if (joined.faceCount() != 10 || joined.vertexCount() != 40
                || joined.indexCount() != 60)
            return "FAIL: adjacent blocks must hide their shared faces";
        if (joined.faceMaterials().length != 10)
            return "FAIL: face material mapping";
        if (build(new RubyDungBlockWorld()).faceCount() != 0)
            return "FAIL: empty world must produce empty mesh";
        return "PASS: exposed-face meshing, shared-face culling, vertex attributes and material mapping.";
    }
}
