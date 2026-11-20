package com.terra.android;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.opengl.GLSurfaceView;

public final class MinecraftPreviewActivity extends Activity {
    private GLSurfaceView surface;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root=new FrameLayout(this);
        surface=new GLSurfaceView(this);
        surface.setEGLContextClientVersion(2);
        surface.setRenderer(new MinecraftPreviewRenderer());
        surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        root.addView(surface,new FrameLayout.LayoutParams(-1,-1));
        TextView title=new TextView(this);
        title.setText("UNIVERSE LAUNCHER  •  FIRST GRAPHICS TEST");
        title.setTextColor(Color.WHITE);title.setTextSize(13);title.setPadding(18,18,18,18);
        title.setBackgroundColor(0x99000000);
        FrameLayout.LayoutParams top=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);
        root.addView(title,top);
        TextView footer=new TextView(this);
        footer.setText("SOLARCRYSTAL 0.8.8b  |  EXPERIMENTAL VOXEL RENDERER");
        footer.setTextColor(Color.WHITE);footer.setTextSize(11);footer.setPadding(12,12,12,12);
        footer.setBackgroundColor(0x99000000);
        FrameLayout.LayoutParams bottom=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);
        root.addView(footer,bottom);
        setContentView(root);
    }
    @Override protected void onResume(){super.onResume();if(surface!=null)surface.onResume();}
    @Override protected void onPause(){if(surface!=null)surface.onPause();super.onPause();}
}