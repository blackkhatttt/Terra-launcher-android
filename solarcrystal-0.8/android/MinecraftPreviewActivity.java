package com.terra.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.opengl.GLSurfaceView;

public final class MinecraftPreviewActivity extends Activity {
    private GLSurfaceView surface;
    private static final int IMMERSIVE_FLAGS =
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        | View.SYSTEM_UI_FLAG_FULLSCREEN
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(IMMERSIVE_FLAGS);
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);

        FrameLayout root = new FrameLayout(this);
        surface = new GLSurfaceView(this);
        surface.setEGLContextClientVersion(2);
        surface.setRenderer(new MinecraftPreviewRenderer());
        surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));

        TextView title = new TextView(this);
        title.setText("UNIVERSE LAUNCHER  •  GRAPHICS PREVIEW");
        title.setTextColor(Color.WHITE);
        title.setTextSize(12);
        title.setPadding(16, 10, 16, 10);
        title.setBackgroundColor(0x88000000);
        FrameLayout.LayoutParams top = new FrameLayout.LayoutParams(
            -1, -2, Gravity.TOP);
        root.addView(title, top);

        TextView footer = new TextView(this);
        footer.setText("SOLARCRYSTAL 0.8.8c  |  OPTIMIZED VOXEL PREVIEW");
        footer.setTextColor(Color.WHITE);
        footer.setTextSize(10);
        footer.setPadding(12, 8, 12, 8);
        footer.setBackgroundColor(0x88000000);
        FrameLayout.LayoutParams bottom = new FrameLayout.LayoutParams(
            -1, -2, Gravity.BOTTOM);
        root.addView(footer, bottom);
        setContentView(root);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) getWindow().getDecorView().setSystemUiVisibility(IMMERSIVE_FLAGS);
    }

    @Override protected void onResume() {
        super.onResume();
        if (surface != null) surface.onResume();
        getWindow().getDecorView().setSystemUiVisibility(IMMERSIVE_FLAGS);
    }

    @Override protected void onPause() {
        if (surface != null) surface.onPause();
        super.onPause();
    }
}
