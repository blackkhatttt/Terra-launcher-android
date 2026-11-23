package com.terra.android;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import com.universe.solarcrystal.RubyDungBlockWorld;
import com.universe.solarcrystal.RubyDungVoxelMeshData;
import com.universe.solarcrystal.RubyDungVoxelMesher;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Uploads the RubyDung CPU voxel mesh format to OpenGL ES 2.0 and draws it.
 * The supplied world is a generated renderer-test scene, not RubyDung gameplay.
 */
final class RubyDungVoxelRenderer implements GLSurfaceView.Renderer {
    private static final String VERTEX_SHADER =
        "attribute vec3 aPosition; attribute vec3 aNormal; attribute vec4 aColor; " +
        "uniform mat4 uMvp; varying vec4 vColor; varying float vLight; " +
        "void main(){ gl_Position=uMvp*vec4(aPosition,1.0); " +
        "vColor=aColor; vLight=0.62+0.38*max(dot(normalize(aNormal)," +
        "normalize(vec3(-0.35,0.85,0.40))),0.0); }";
    private static final String FRAGMENT_SHADER =
        "precision mediump float; varying vec4 vColor; varying float vLight; " +
        "void main(){ gl_FragColor=vec4(vColor.rgb*vLight,vColor.a); }";

    private int program;
    private int positionHandle;
    private int normalHandle;
    private int colorHandle;
    private int mvpHandle;
    private int[] buffers = new int[4];
    private int indexCount;
    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] mvp = new float[16];
    private volatile float yaw = 38f;
    private volatile float pitch = 30f;
    private volatile float distance = 24f;

    void setOrbit(float nextYaw, float nextPitch) {
        yaw = nextYaw;
        pitch = Math.max(12f, Math.min(68f, nextPitch));
    }

    void zoomBy(float scale) {
        if (scale > 0f) distance = Math.max(12f, Math.min(42f, distance / scale));
    }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.48f, 0.70f, 0.91f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        program = linkProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        normalHandle = GLES20.glGetAttribLocation(program, "aNormal");
        colorHandle = GLES20.glGetAttribLocation(program, "aColor");
        mvpHandle = GLES20.glGetUniformLocation(program, "uMvp");
        uploadWorld();
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        GLES20.glViewport(0, 0, safeWidth, safeHeight);
        Matrix.perspectiveM(projection, 0, 55f,
            (float) safeWidth / (float) safeHeight, 0.1f, 120f);
    }

    @Override public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (program == 0 || indexCount == 0) return;
        double angle = Math.toRadians(yaw);
        double elevation = Math.toRadians(pitch);
        float horizontal = (float) (Math.cos(elevation) * distance);
        float eyeX = (float) (Math.sin(angle) * horizontal);
        float eyeZ = (float) (Math.cos(angle) * horizontal);
        float eyeY = (float) (Math.sin(elevation) * distance);
        Matrix.setLookAtM(view, 0, eyeX, eyeY + 1.2f, eyeZ,
            0f, 1.2f, 0f, 0f, 1f, 0f);
        Matrix.multiplyMM(mvp, 0, projection, 0, view, 0);

        GLES20.glUseProgram(program);
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT,
            false, 0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]);
        GLES20.glEnableVertexAttribArray(normalHandle);
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
            false, 0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[2]);
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT,
            false, 0, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[3]);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount,
            GLES20.GL_UNSIGNED_SHORT, 0);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(normalHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void uploadWorld() {
        RubyDungVoxelBlockWorldFactory factory = new RubyDungVoxelBlockWorldFactory();
        RubyDungBlockWorld world = factory.createTestWorld();
        RubyDungVoxelMeshData data =
            RubyDungVoxelMeshData.from(RubyDungVoxelMesher.build(world));

        FloatBuffer positions = floats(data.positions());
        FloatBuffer normals = floats(data.normals());
        FloatBuffer colors = floats(data.colors());
        ShortBuffer indices = shorts(data.indices());
        indexCount = data.indexCount();

        GLES20.glGenBuffers(4, buffers, 0);
        uploadBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0], positions,
            positions.capacity() * 4);
        uploadBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1], normals,
            normals.capacity() * 4);
        uploadBuffer(GLES20.GL_ARRAY_BUFFER, buffers[2], colors,
            colors.capacity() * 4);
        uploadBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[3], indices,
            indices.capacity() * 2);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private static void uploadBuffer(int target, int id, java.nio.Buffer data, int size) {
        GLES20.glBindBuffer(target, id);
        GLES20.glBufferData(target, size, data, GLES20.GL_STATIC_DRAW);
    }

    private static FloatBuffer floats(float[] values) {
        FloatBuffer result = ByteBuffer.allocateDirect(values.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        result.put(values).position(0);
        return result;
    }

    private static ShortBuffer shorts(short[] values) {
        ShortBuffer result = ByteBuffer.allocateDirect(values.length * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer();
        result.put(values).position(0);
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
            throw new RuntimeException("OpenGL link failed: " + GLES20.glGetProgramInfoLog(result));
        return result;
    }
}
