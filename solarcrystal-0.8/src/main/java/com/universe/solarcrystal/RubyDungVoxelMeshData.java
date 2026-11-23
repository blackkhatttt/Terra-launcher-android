package com.universe.solarcrystal;

/**
 * Converts CPU voxel geometry into arrays ready for an OpenGL ES upload.
 * This class has no Android or OpenGL dependency and can be tested on the JVM.
 */
public final class RubyDungVoxelMeshData {
    private final float[] positions;
    private final float[] normals;
    private final float[] colors;
    private final short[] indices;

    private RubyDungVoxelMeshData(float[] positions, float[] normals,
                                  float[] colors, short[] indices) {
        this.positions = positions;
        this.normals = normals;
        this.colors = colors;
        this.indices = indices;
    }

    public static RubyDungVoxelMeshData from(RubyDungVoxelMesher.Mesh mesh) {
        if (mesh == null) throw new IllegalArgumentException("Mesh must not be null");
        float[] positions = mesh.positions();
        float[] normals = mesh.normals();
        int[] sourceIndices = mesh.indices();
        String[] materials = mesh.faceMaterials();

        if (positions.length % 3 != 0 || normals.length != positions.length
                || sourceIndices.length != materials.length * 6
                || positions.length / 3 != materials.length * 4) {
            throw new IllegalArgumentException("Voxel mesh attribute lengths are inconsistent");
        }
        int vertexCount = positions.length / 3;
        if (vertexCount > 65535)
            throw new IllegalArgumentException("Mesh exceeds unsigned-short index capacity");

        float[] colors = new float[vertexCount * 4];
        for (int face = 0; face < materials.length; face++) {
            float[] base = colorFor(materials[face]);
            for (int corner = 0; corner < 4; corner++) {
                int vertex = face * 4 + corner;
                float nx = normals[vertex * 3];
                float ny = normals[vertex * 3 + 1];
                float nz = normals[vertex * 3 + 2];
                float shade = ny > 0.5f ? 1.0f : (ny < -0.5f ? 0.48f
                    : (nz > 0.5f ? 0.82f : (nz < -0.5f ? 0.70f : 0.88f)));
                int offset = vertex * 4;
                colors[offset] = base[0] * shade;
                colors[offset + 1] = base[1] * shade;
                colors[offset + 2] = base[2] * shade;
                colors[offset + 3] = 1.0f;
            }
        }

        short[] indices = new short[sourceIndices.length];
        for (int i = 0; i < sourceIndices.length; i++) {
            int index = sourceIndices[i];
            if (index < 0 || index >= vertexCount)
                throw new IllegalArgumentException("Voxel mesh index out of range: " + index);
            indices[i] = (short) index;
        }
        return new RubyDungVoxelMeshData(positions.clone(), normals.clone(), colors, indices);
    }

    private static float[] colorFor(String material) {
        if ("grass".equals(material)) return new float[] {0.30f, 0.68f, 0.24f};
        if ("dirt".equals(material)) return new float[] {0.48f, 0.31f, 0.17f};
        if ("stone".equals(material)) return new float[] {0.53f, 0.55f, 0.57f};
        if ("wood".equals(material)) return new float[] {0.48f, 0.29f, 0.13f};
        if ("leaves".equals(material)) return new float[] {0.16f, 0.48f, 0.18f};
        return new float[] {0.72f, 0.34f, 0.72f};
    }

    public int vertexCount() { return positions.length / 3; }
    public int indexCount() { return indices.length; }
    public float[] positions() { return positions.clone(); }
    public float[] normals() { return normals.clone(); }
    public float[] colors() { return colors.clone(); }
    public short[] indices() { return indices.clone(); }

    public static String selfTest() {
        RubyDungBlockWorld one = new RubyDungBlockWorld()
            .withBlock(new RubyDungBlockWorld.Block(0, 0, 0, "grass"));
        RubyDungVoxelMeshData data = from(RubyDungVoxelMesher.build(one));
        if (data.vertexCount() != 24 || data.indexCount() != 36)
            return "FAIL: GPU data counts for one block";
        if (data.colors.length != 96)
            return "FAIL: RGBA color data length";
        for (short index : data.indices) {
            int unsigned = index & 0xffff;
            if (unsigned >= data.vertexCount())
                return "FAIL: unsigned-short index range";
        }
        float[] colors = data.colors();
        if (colors[1] <= 0.0f || colors[3] != 1.0f)
            return "FAIL: material color conversion";
        return "PASS: vertex attributes, material colors and unsigned-short indices.";
    }
}
