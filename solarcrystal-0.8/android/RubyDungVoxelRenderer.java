package com.terra.android;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import com.universe.solarcrystal.RubyDungBlockWorld;
import com.universe.solarcrystal.RubyDungVoxelMeshData;
import com.universe.solarcrystal.RubyDungVoxelMesher;
import com.universe.solarcrystal.RubyDungVoxelBlockWorldFactory;
import com.universe.solarcrystal.RubyDungVoxelPlayerController;
import com.universe.solarcrystal.RubyDungRayPicker;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** GLES2 renderer for a generated voxel test world, with a separate test player controller. */
final class RubyDungVoxelRenderer implements GLSurfaceView.Renderer {
    private static final String VERTEX_SHADER =
        "attribute vec3 aPosition; attribute vec3 aNormal; attribute vec4 aColor; " +
        "uniform mat4 uMvp; varying vec4 vColor; varying float vLight; " +
        "void main(){ gl_Position=uMvp*vec4(aPosition,1.0); vColor=aColor; " +
        "vLight=0.62+0.38*max(dot(normalize(aNormal),normalize(vec3(-0.35,0.85,0.40))),0.0); }";
    private static final String FRAGMENT_SHADER =
        "precision mediump float; varying vec4 vColor; varying float vLight; " +
        "void main(){ gl_FragColor=vec4(vColor.rgb*vLight,vColor.a); }";

    private int program, positionHandle, normalHandle, colorHandle, mvpHandle;
    private final int[] buffers = new int[4];
    private int indexCount;
    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] mvp = new float[16];
    private RubyDungVoxelPlayerController player;
    private RubyDungBlockWorld world;
    private static final String[] PLACEABLE = { "grass", "dirt", "stone", "wood", "leaves" };
    private int selectedBlock;
    private volatile float forwardInput, strafeInput;\n    private volatile float lookHorizontal, lookVertical;
    private volatile boolean jumpRequested;
    private long lastFrameNanos;

    RubyDungVoxelRenderer() {
        world = new RubyDungVoxelBlockWorldFactory().createTestWorld();
        player = new RubyDungVoxelPlayerController(world, 0.5, 5.0, 0.5);
        RubyDungVoxelMeshData data = RubyDungVoxelMeshData.from(RubyDungVoxelMesher.build(world));
        FloatBuffer positions = floats(data.positions());
        FloatBuffer normals = floats(data.normals());
        FloatBuffer colors = floats(data.colors());
        ShortBuffer indices = shorts(data.indices());
        indexCount = data.indexCount();
        pendingPositions = positions;
        pendingNormals = normals;
        pendingColors = colors;
        pendingIndices = indices;
    }

    private final FloatBuffer pendingPositions, pendingNormals, pendingColors;
    private final ShortBuffer pendingIndices;

    void setMovement(float forward, float strafe) {
        forwardInput = Math.max(-1f, Math.min(1f, forward));
        strafeInput = Math.max(-1f, Math.min(1f, strafe));
    }
    void jump() { jumpRequested = true; }\n    void setLookInput(float horizontal, float vertical) {\n        lookHorizontal = Math.max(-1f, Math.min(1f, horizontal));\n        lookVertical = Math.max(-1f, Math.min(1f, vertical));\n    }
    void rotate(float yawDelta, float pitchDelta) {
        player.rotate(yawDelta, pitchDelta);
    }


    void breakBlock() {
        RubyDungBlockWorld.Selection hit = aimedBlock();
        if (hit == null) return;
        world = world.withoutBlock(hit.block.x, hit.block.y, hit.block.z);
        rebuildMesh();
    }

    void placeBlock() {
        RubyDungBlockWorld.Selection hit = aimedBlock();
        if (hit == null) return;
        double px = hit.point.x - hit.block.x;
        double py = hit.point.y - hit.block.y;
        double pz = hit.point.z - hit.block.z;
        double[] distances = {Math.abs(1.0 - px), Math.abs(px),
            Math.abs(1.0 - py), Math.abs(py), Math.abs(1.0 - pz), Math.abs(pz)};
        int face = 0;
        for (int i = 1; i < distances.length; i++)
            if (distances[i] < distances[face]) face = i;
        int[][] offsets = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        int bx = hit.block.x + offsets[face][0];
        int by = hit.block.y + offsets[face][1];
        int bz = hit.block.z + offsets[face][2];
        if (world.getBlock(bx, by, bz) != null) return;
        if (bx >= player.x() - 0.30 && bx <= player.x() + 0.30
                && by < player.y() + 1.75 && by + 1.0 > player.y()
                && bz >= player.z() - 0.30 && bz <= player.z() + 0.30) return;
        world = world.withBlock(new RubyDungBlockWorld.Block(
            bx, by, bz, PLACEABLE[selectedBlock]));
        rebuildMesh();
    }

    void nextBlock() {
        selectedBlock = (selectedBlock + 1) % PLACEABLE.length;
    }

    private RubyDungBlockWorld.Selection aimedBlock() {
        double yaw = Math.toRadians(player.yaw());
        double pitch = Math.toRadians(player.pitch());
        RubyDungRayPicker.Vec3 origin = new RubyDungRayPicker.Vec3(
            player.x(), player.y() + 1.55, player.z());
        RubyDungRayPicker.Vec3 direction = new RubyDungRayPicker.Vec3(
            Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch),
            -Math.cos(yaw) * Math.cos(pitch));
        return world.pick(origin, direction, 6.0);
    }

    private void rebuildMesh() {
        RubyDungVoxelMeshData data = RubyDungVoxelMeshData.from(
            RubyDungVoxelMesher.build(world));
        FloatBuffer positions = floats(data.positions());
        FloatBuffer normals = floats(data.normals());
        FloatBuffer colors = floats(data.colors());
        ShortBuffer indices = shorts(data.indices());
        indexCount = data.indexCount();
        uploadBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0], positions, positions.capacity() * 4);
        uploadBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1], normals, normals.capacity() * 4);
        uploadBuffer(GLES20.GL_ARRAY_BUFFER, buffers[2], colors, colors.capacity() * 4);
        uploadBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[3], indices, indices.capacity() * 2);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
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
        GLES20.glGenBuffers(4, buffers, 0);
        uploadBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0], pendingPositions, pendingPositions.capacity() * 4);
        uploadBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1], pendingNormals, pendingNormals.capacity() * 4);
        uploadBuffer(GLES20.GL_ARRAY_BUFFER, buffers[2], pendingColors, pendingColors.capacity() * 4);
        uploadBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[3], pendingIndices, pendingIndices.capacity() * 2);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        lastFrameNanos = System.nanoTime();
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        int w = Math.max(1, width), h = Math.max(1, height);
        GLES20.glViewport(0, 0, w, h);
        Matrix.perspectiveM(projection, 0, 70f, (float) w / h, 0.1f, 120f);
    }

    @Override public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        double dt = Math.min(0.05, Math.max(0.0, (now - lastFrameNanos) / 1_000_000_000.0));
        lastFrameNanos = now;
        player.tick(forwardInput, strafeInput, jumpRequested, dt);\n        player.rotate(lookHorizontal * (float) (125.0 * dt),\n            -lookVertical * (float) (105.0 * dt));
        jumpRequested = false;

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (program == 0 || indexCount == 0) return;
        double yaw = Math.toRadians(player.yaw());
        double pitch = Math.toRadians(player.pitch());
        float eyeX = (float) player.x();
        float eyeY = (float) (player.y() + 1.55);
        float eyeZ = (float) player.z();
        float lookX = (float) (Math.sin(yaw) * Math.cos(pitch));
        float lookY = (float) Math.sin(pitch);
        float lookZ = (float) (-Math.cos(yaw) * Math.cos(pitch));
        Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ,
            eyeX + lookX, eyeY + lookY, eyeZ + lookZ, 0f, 1f, 0f);
        Matrix.multiplyMM(mvp, 0, projection, 0, view, 0);

        GLES20.glUseProgram(program);
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]);
        GLES20.glEnableVertexAttribArray(normalHandle);
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[2]);
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[3]);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(normalHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private static void uploadBuffer(int target, int id, java.nio.Buffer data, int size) {
        GLES20.glBindBuffer(target, id);
        GLES20.glBufferData(target, size, data, GLES20.GL_STATIC_DRAW);
    }
    private static FloatBuffer floats(float[] values) {
        FloatBuffer b = ByteBuffer.allocateDirect(values.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        b.put(values).position(0); return b;
    }
    private static ShortBuffer shorts(short[] values) {
        ShortBuffer b = ByteBuffer.allocateDirect(values.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        b.put(values).position(0); return b;
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
        if (status[0] == 0) throw new RuntimeException("OpenGL link failed: " + GLES20.glGetProgramInfoLog(result));
        return result;
    }
}
