package com.terra.android;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

final class MinecraftPreviewRenderer implements GLSurfaceView.Renderer {
    private static final String VERTEX =
        "attribute vec3 aPosition; attribute vec4 aColor; uniform mat4 uMVP; varying vec4 vColor; " +
        "void main(){ gl_Position=uMVP*vec4(aPosition,1.0); vColor=aColor; }";
    private static final String FRAGMENT =
        "precision mediump float; varying vec4 vColor; void main(){ gl_FragColor=vColor; }";
    private int program, positionHandle, colorHandle, mvpHandle;
    private FloatBuffer vertices, colors;
    private final float[] projection=new float[16], view=new float[16], model=new float[16], vp=new float[16], mvp=new float[16];
    private Cube grass, dirt, stone, wood, leaves;
    private int width=1, height=1;

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.48f,0.72f,0.96f,1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        program=linkProgram(VERTEX,FRAGMENT);
        positionHandle=GLES20.glGetAttribLocation(program,"aPosition");
        colorHandle=GLES20.glGetAttribLocation(program,"aColor");
        mvpHandle=GLES20.glGetUniformLocation(program,"uMVP");
        grass=new Cube(new float[]{0.25f,0.68f,0.22f},new float[]{0.48f,0.31f,0.16f});
        dirt=new Cube(new float[]{0.48f,0.31f,0.16f},new float[]{0.38f,0.23f,0.12f});
        stone=new Cube(new float[]{0.55f,0.57f,0.59f},new float[]{0.42f,0.44f,0.46f});
        wood=new Cube(new float[]{0.48f,0.31f,0.16f},new float[]{0.34f,0.20f,0.09f});
        leaves=new Cube(new float[]{0.12f,0.48f,0.16f},new float[]{0.08f,0.34f,0.12f});
    }

    @Override public void onSurfaceChanged(GL10 gl,int w,int h) {
        width=Math.max(1,w); height=Math.max(1,h);
        GLES20.glViewport(0,0,width,height);
        Matrix.perspectiveM(projection,0,55f,(float)width/(float)height,0.1f,100f);
    }

    @Override public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);
        Matrix.setLookAtM(view,0,12f,10f,16f,0f,0.5f,0f,0f,1f,0f);
        Matrix.multiplyMM(vp,0,projection,0,view,0);
        for(int x=-6;x<=6;x++) for(int z=-6;z<=6;z++) {
            int h=terrainHeight(x,z);
            for(int y=0;y<h;y++) draw(x,y,z,y==h-1?grass:dirt);
            if ((x*7+z*11)%17==0 && h<3) {
                for(int y=h;y<h+3;y++) draw(x,y,z,wood);
                for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++)
                    for(int dy=2;dy<=3;dy++) if(Math.abs(dx)+Math.abs(dz)+Math.abs(dy-2)<=3)
                        draw(x+dx,h+dy,z+dz,leaves);
            }
        }
        for(int x=-2;x<=2;x++) draw(x,terrainHeight(x,0),0,stone);
    }

    private int terrainHeight(int x,int z) {
        double hill=Math.sin(x*0.55)*0.8+Math.cos(z*0.43)*0.65+Math.sin((x+z)*0.3)*0.45;
        return 1+Math.max(0,(int)Math.round(hill+0.5));
    }

    private void draw(float x,float y,float z,Cube cube) {
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.multiplyMM(mvp,0,vp,0,model,0);
        GLES20.glUniformMatrix4fv(mvpHandle,1,false,mvp,0);
        cube.bindAndDraw(positionHandle,colorHandle);
    }

    private static int compile(int type,String source) {
        int shader=GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader,source);
        GLES20.glCompileShader(shader);
        int[] status=new int[1]; GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,status,0);
        if(status[0]==0){String log=GLES20.glGetShaderInfoLog(shader);GLES20.glDeleteShader(shader);throw new RuntimeException("Shader compile failed: "+log);}
        return shader;
    }
    private static int linkProgram(String vertex,String fragment) {
        int p=GLES20.glCreateProgram();
        GLES20.glAttachShader(p,compile(GLES20.GL_VERTEX_SHADER,vertex));
        GLES20.glAttachShader(p,compile(GLES20.GL_FRAGMENT_SHADER,fragment));
        GLES20.glLinkProgram(p);
        int[] status=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,status,0);
        if(status[0]==0)throw new RuntimeException("OpenGL program link failed: "+GLES20.glGetProgramInfoLog(p));
        return p;
    }

    private static final class Cube {
        private static final float[][] FACES={
            {-0.5f,0.5f,-0.5f, 0.5f,0.5f,-0.5f, 0.5f,0.5f,0.5f, -0.5f,0.5f,0.5f},
            {-0.5f,-0.5f,0.5f, 0.5f,-0.5f,0.5f, 0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f},
            {-0.5f,-0.5f,0.5f, -0.5f,0.5f,0.5f, -0.5f,0.5f,-0.5f, -0.5f,-0.5f,-0.5f},
            {0.5f,-0.5f,-0.5f, 0.5f,0.5f,-0.5f, 0.5f,0.5f,0.5f, 0.5f,-0.5f,0.5f},
            {-0.5f,-0.5f,0.5f, -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,-0.5f,0.5f},
            {-0.5f,0.5f,-0.5f, -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,0.5f,-0.5f}
        };
        private final FloatBuffer vb,cb;
        Cube(float[] top,float[] side) {
            float[] v=new float[36*3],c=new float[36*4]; int vi=0,ci=0;
            for(int face=0;face<6;face++){
                int[] order={0,1,2,0,2,3};
                float[] base=face==0?top:side;
                float shade=face==0?1f:(face==1?0.62f:(face>=2&&face<=3?0.82f:0.72f));
                for(int index:order){
                    int offset=index*3;
                    v[vi++]=FACES[face][offset];v[vi++]=FACES[face][offset+1];v[vi++]=FACES[face][offset+2];
                    c[ci++]=base[0]*shade;c[ci++]=base[1]*shade;c[ci++]=base[2]*shade;c[ci++]=1f;
                }
            }
            vb=ByteBuffer.allocateDirect(v.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();vb.put(v).position(0);
            cb=ByteBuffer.allocateDirect(c.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();cb.put(c).position(0);
        }
        void bindAndDraw(int pos,int col) {
            vb.position(0);cb.position(0);
            GLES20.glEnableVertexAttribArray(pos);GLES20.glEnableVertexAttribArray(col);
            GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,0,vb);
            GLES20.glVertexAttribPointer(col,4,GLES20.GL_FLOAT,false,0,cb);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,36);
        }
    }
}