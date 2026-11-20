package com.terra.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.opengl.GLSurfaceView;

public final class MinecraftPreviewActivity extends Activity {
    private GLSurfaceView surface;
    private MinecraftPreviewRenderer renderer;
    private float lastX, lastY, lastPinchDistance;
    private float cameraYaw = 38f, cameraPitch = 28f;
    private boolean dragging;
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
        renderer = new MinecraftPreviewRenderer();
        surface.setRenderer(renderer);
        surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        surface.setOnTouchListener((view, event) -> handlePreviewTouch(event));
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));

        TextView title = new TextView(this);
        title.setText("UNIVERSE LAUNCHER  •  INTERACTIVE GRAPHICS TEST");
        title.setTextColor(Color.WHITE);
        title.setTextSize(12);
        title.setPadding(16, 10, 16, 10);
        title.setBackgroundColor(0x88000000);
        root.addView(title, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        TextView footer = new TextView(this);
        footer.setText("SOLARCRYSTAL 0.8.8d  |  DRAG TO ROTATE  •  PINCH TO ZOOM");
        footer.setTextColor(Color.WHITE);
        footer.setTextSize(10);
        footer.setPadding(12, 8, 12, 8);
        footer.setBackgroundColor(0x88000000);
        root.addView(footer, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        setContentView(root);
    }

    private boolean handlePreviewTouch(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            lastX = event.getX();
            lastY = event.getY();
            dragging = true;
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            if (event.getPointerCount() >= 2)
                lastPinchDistance = pointerDistance(event);
            dragging = false;
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (event.getPointerCount() >= 2) {
                float nextDistance = pointerDistance(event);
                if (lastPinchDistance > 0f && nextDistance > 0f) {
                    float scale = nextDistance / lastPinchDistance;
                    surface.queueEvent(() -> renderer.zoomBy(scale));
                }
                lastPinchDistance = nextDistance;
            } else if (dragging) {
                float x = event.getX();
                float y = event.getY();
                float dx = x - lastX;
                float dy = y - lastY;
                cameraYaw += dx * 0.45f;
                cameraPitch -= dy * 0.30f;
                cameraPitch = Math.max(12f, Math.min(65f, cameraPitch));
                final float yaw = cameraYaw;
                final float pitch = cameraPitch;
                surface.queueEvent(() -> renderer.setOrbit(yaw, pitch));
                lastX = x;
                lastY = y;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_UP) {
            lastPinchDistance = 0f;
            if (event.getPointerCount() > 1) {
                int remaining = event.getActionIndex() == 0 ? 1 : 0;
                lastX = event.getX(remaining);
                lastY = event.getY(remaining);
                dragging = true;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            dragging = false;
            lastPinchDistance = 0f;
            return true;
        }
        return true;
    }

    private static float pointerDistance(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0f;
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
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
