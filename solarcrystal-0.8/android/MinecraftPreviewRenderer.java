package com.terra.android;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Optimized generated voxel scene used only as a SolarCrystal graphics proof. */
final class MinecraftPreviewRenderer implements GLSurfaceView.Renderer {
    private static final String VERTEX =
        "attribute vec3 aPosition; attribute vec4 aColor; uniform mat4 uMVP; varying vec4 vColor; " +
        "void main(){ gl_Position=uMVP*vec4(aPosition,1.0); vColor=aColor; }";
    private static final String FRAGMENT =
        "precision mediump float; varying vec4 vColor; void main(){ gl_FragColor=vColor; }";

    private int program, positionHandle, colorHandle, mvpHandle;
    private FloatBuffer vertexBuffer, colorBuffer;
    private int vertexCount;
    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] vp = new float[16];
    private int surfaceWidth = 1, surfaceHeight = 1;

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.48f, 0.72f, 0.96f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // Disable face culling: all six cube faces must remain visible on GLES drivers.
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        program = linkProgram(VERTEX, FRAGMENT);
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        colorHandle = GLES20.glGetAttribLocation(program, "aColor");
        mvpHandle = GLES20.glGetUniformLocation(program, "uMVP");
        buildWorldMesh();
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        surfaceWidth = Math.max(1, width);
        surfaceHeight = Math.max(1, height);
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
        Matrix.perspectiveM(projection, 0, 52f,
            (float) surfaceWidth / (float) surfaceHeight, 0.1f, 100f);
    }

    @Override public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);

        Matrix.setLookAtM(view, 0,
            11.5f, 9.5f, 14.5f,
            0f, 0.7f, 0f,
            0f, 1f, 0f);
        Matrix.multiplyMM(vp, 0, projection, 0, view, 0);
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, vp, 0);

        vertexBuffer.position(0);
        colorBuffer.position(0);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
    }

    private void buildWorldMesh() {
        ArrayList<Float> positions = new ArrayList<>();
        ArrayList<Float> colors = new ArrayList<>();
        float[] grassTop = {0.30f, 0.70f, 0.25f};
        float[] grassSide = {0.43f, 0.29f, 0.15f};
        float[] dirtTop = {0.48f, 0.32f, 0.18f};
        float[] dirtSide = {0.38f, 0.24f, 0.13f};
        float[] trunk = {0.43f, 0.27f, 0.13f};
        float[] leaf = {0.12f, 0.48f, 0.17f};
        float[] stone = {0.55f, 0.57f, 0.59f};

        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                int height = terrainHeight(x, z);
                for (int y = 0; y < height; y++) {
                    boolean top = y == height - 1;
                    appendCube(positions, colors, x, y, z,
                        top ? grassTop : dirtTop, top ? grassSide : dirtSide);
                }
            }
        }

        // A few smaller, separated trees with compact canopies.
        int[][] trees = {{-4, -3}, {2, -4}, {4, 2}, {-2, 4}};
        for (int[] tree : trees) {
            int x = tree[0], z = tree[1], base = terrainHeight(x, z);
            for (int y = 0; y < 2; y++)
                appendCube(positions, colors, x, base + y, z, trunk, trunk);
            int crown = base + 2;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= 2)
                        appendCube(positions, colors, x + dx, crown, z + dz, leaf, leaf);
                }
            }
            appendCube(positions, colors, x, crown + 1, z, leaf, leaf);
        }

        // A short stone patch gives the scene a second block material.
        for (int x = -1; x <= 1; x++)
            appendCube(positions, colors, x, terrainHeight(x, 0), 0, stone, stone);

        float[] vertexData = toArray(positions);
        float[] colorData = toArray(colors);
        vertexCount = vertexData.length / 3;
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(vertexData).position(0);
        colorBuffer = ByteBuffer.allocateDirect(colorData.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorBuffer.put(colorData).position(0);
    }

    private int terrainHeight(int x, int z) {
        double waves = Math.sin(x * 0.38) * 0.45
            + Math.cos(z * 0.34) * 0.38
            + Math.sin((x + z) * 0.22) * 0.30;
        return 1 + Math.max(0, (int) Math.round(waves + 0.45));
    }

    private static void appendCube(ArrayList<Float> vertices, ArrayList<Float> colors,
                                   float x, float y, float z,
                                   float[] topColor, float[] sideColor) {
        final float[][] faces = {
            {-0.5f,0.5f,-0.5f, 0.5f,0.5f,-0.5f, 0.5f,0.5f,0.5f, -0.5f,0.5f,0.5f},
            {-0.5f,-0.5f,0.5f, 0.5f,-0.5f,0.5f, 0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f},
            {-0.5f,-0.5f,0.5f, -0.5f,0.5f,0.5f, -0.5f,0.5f,-0.5f, -0.5f,-0.5f,-0.5f},
            {0.5f,-0.5f,-0.5f, 0.5f,0.5f,-0.5f, 0.5f,0.5f,0.5f, 0.5f,-0.5f,0.5f},
            {-0.5f,-0.5f,0.5f, -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,-0.5f,0.5f},
            {-0.5f,0.5f,-0.5f, -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,0.5f,-0.5f}
        };
        final int[] order = {0, 1, 2, 0, 2, 3};
        for (int face = 0; face < 6; face++) {
            float[] base = face == 0 ? topColor : sideColor;
            float shade = face == 0 ? 1f : (face == 1 ? 0.62f :
                (face == 2 || face == 3 ? 0.82f : 0.72f));
            for (int index : order) {
                int offset = index * 3;
                vertices.add(x + faces[face][offset]);
                vertices.add(y + faces[face][offset + 1]);
                vertices.add(z + faces[face][offset + 2]);
                colors.add(base[0] * shade);
                colors.add(base[1] * shade);
                colors.add(base[2] * shade);
                colors.add(1f);
            }
        }
    }

    private static float[] toArray(ArrayList<Float> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) result[i] = values.get(i);
        return result;
    }

    private static int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Shader compile failed: " + log);
        }
        return shader;
    }

    private static int linkProgram(String vertex, String fragment) {
        int result = GLES20.glCreateProgram();
        GLES20.glAttachShader(result, compileShader(GLES20.GL_VERTEX_SHADER, vertex));
        GLES20.glAttachShader(result, compileShader(GLES20.GL_FRAGMENT_SHADER, fragment));
        GLES20.glLinkProgram(result);
        int[] status = new int[1];
        GLES20.glGetProgramiv(result, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0)
            throw new RuntimeException("OpenGL program link failed: " + GLES20.glGetProgramInfoLog(result));
        return result;
    }
}
