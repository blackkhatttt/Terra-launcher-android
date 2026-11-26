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
import android.widget.Button;
import android.opengl.GLSurfaceView;

public final class MinecraftPreviewActivity extends Activity {
    private GLSurfaceView surface;
    private RubyDungVoxelRenderer renderer;
    private float downX, downY;
    private boolean moving, looking;
    private static final int IMMERSIVE_FLAGS =
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(IMMERSIVE_FLAGS);
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);

        FrameLayout root = new FrameLayout(this);
        surface = new GLSurfaceView(this);
        surface.setEGLContextClientVersion(2);
        renderer = new RubyDungVoxelRenderer();
        surface.setRenderer(renderer);
        surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        surface.setOnTouchListener((view, event) -> handleTouch(event));
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));

        TextView title = label("SOLARCRYSTAL 0.9.11  •  FIRST-PERSON VOXEL TEST");
        root.addView(title, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        TextView help = label("LEFT SIDE: MOVE  •  RIGHT SIDE: LOOK");
        FrameLayout.LayoutParams helpParams = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        root.addView(help, helpParams);

        Button jump = new Button(this);
        jump.setText("JUMP");
        FrameLayout.LayoutParams jumpParams = new FrameLayout.LayoutParams(-2, -2,
            Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        jumpParams.setMargins(0, 0, 18, 0);
        root.addView(jump, jumpParams);
        jump.setOnClickListener(v -> surface.queueEvent(() -> renderer.jump()));
        setContentView(root);
    }

    private TextView label(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(Color.WHITE);
        text.setTextSize(11);
        text.setPadding(14, 10, 14, 10);
        text.setBackgroundColor(0x88000000);
        return text;
    }

    private boolean handleTouch(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            moving = downX < surface.getWidth() * 0.5f;
            looking = !moving;
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            float dx = event.getX() - downX;
            float dy = event.getY() - downY;
            if (moving) {
                float forward = Math.max(-1f, Math.min(1f, -dy / 90f));
                float strafe = Math.max(-1f, Math.min(1f, dx / 90f));
                surface.queueEvent(() -> renderer.setMovement(forward, strafe));
            } else if (looking) {
                float yawDelta = dx * 0.18f;
                float pitchDelta = -dy * 0.14f;
                surface.queueEvent(() -> renderer.rotate(yawDelta, pitchDelta));
                downX = event.getX();
                downY = event.getY();
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (moving) surface.queueEvent(() -> renderer.setMovement(0f, 0f));
            moving = false;
            looking = false;
            return true;
        }
        return true;
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
