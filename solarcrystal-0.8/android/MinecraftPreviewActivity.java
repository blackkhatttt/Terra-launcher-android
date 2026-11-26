package com.terra.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.opengl.GLSurfaceView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

public final class MinecraftPreviewActivity extends Activity {
    private GLSurfaceView surface;
    private RubyDungVoxelRenderer renderer;

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
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));

        TextView title = label("SOLARCRYSTAL 0.9.12  •  FIRST-PERSON VOXEL TEST");
        root.addView(title, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        TextView help = label("LEFT JOYSTICK: MOVE     RIGHT JOYSTICK: LOOK");
        root.addView(help, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(5, 2, 5, 2);
        actions.setBackgroundColor(0x99000000);
        Button breakButton = actionButton("BREAK");
        Button placeButton = actionButton("PLACE");
        Button blockButton = actionButton("BLOCK");
        actions.addView(breakButton);
        actions.addView(placeButton);
        actions.addView(blockButton);
        FrameLayout.LayoutParams actionsParams = new FrameLayout.LayoutParams(
            -2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        actionsParams.setMargins(0, 0, 0, dp(18));
        root.addView(actions, actionsParams);
        breakButton.setOnClickListener(v -> surface.queueEvent(() -> renderer.breakBlock()));
        placeButton.setOnClickListener(v -> surface.queueEvent(() -> renderer.placeBlock()));
        blockButton.setOnClickListener(v -> surface.queueEvent(() -> renderer.nextBlock()));

        JoystickView moveStick = new JoystickView(false);
        FrameLayout.LayoutParams moveParams = new FrameLayout.LayoutParams(
            dp(148), dp(148), Gravity.BOTTOM | Gravity.LEFT);
        moveParams.setMargins(dp(18), 0, 0, dp(12));
        root.addView(moveStick, moveParams);

        JoystickView lookStick = new JoystickView(true);
        FrameLayout.LayoutParams lookParams = new FrameLayout.LayoutParams(
            dp(148), dp(148), Gravity.BOTTOM | Gravity.RIGHT);
        lookParams.setMargins(0, 0, dp(18), dp(12));
        root.addView(lookStick, lookParams);

        Button jump = actionButton("JUMP");
        FrameLayout.LayoutParams jumpParams = new FrameLayout.LayoutParams(-2, -2,
            Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        jumpParams.setMargins(0, 0, dp(12), 0);
        root.addView(jump, jumpParams);
        jump.setOnClickListener(v -> surface.queueEvent(() -> renderer.jump()));

        setContentView(root);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private Button actionButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(10);
        button.setMinWidth(dp(62));
        button.setPadding(dp(4), 0, dp(4), 0);
        return button;
    }

    private TextView label(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(Color.WHITE);
        text.setTextSize(10);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(8), dp(6), dp(8), dp(6));
        text.setBackgroundColor(0x88000000);
        return text;
    }

    private final class JoystickView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean cameraStick;
        private float centerX, centerY, knobX, knobY, radius;
        private float lastTouchX, lastTouchY;
        private boolean active;

        JoystickView(boolean cameraStick) {
            super(MinecraftPreviewActivity.this);
            this.cameraStick = cameraStick;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            centerX = getWidth() * 0.5f;
            centerY = getHeight() * 0.5f;
            radius = Math.min(getWidth(), getHeight()) * 0.43f;
            if (!active) {
                knobX = centerX;
                knobY = centerY;
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x55202020);
            canvas.drawCircle(centerX, centerY, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(0x99FFFFFF);
            canvas.drawCircle(centerX, centerY, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(active ? 0xDDE6E6E6 : 0xAABBBBBB);
            canvas.drawCircle(knobX, knobY, radius * 0.42f, paint);
            paint.setColor(0x66FFFFFF);
            canvas.drawCircle(knobX, knobY, radius * 0.27f, paint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                float x = event.getX();
                float y = event.getY();
                if (action == MotionEvent.ACTION_DOWN) {
                    active = true;
                    lastTouchX = x;
                    lastTouchY = y;
                }
                float dx = x - centerX;
                float dy = y - centerY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float limit = Math.max(1f, radius);
                if (distance > limit) {
                    dx = dx * limit / distance;
                    dy = dy * limit / distance;
                }
                knobX = centerX + dx;
                knobY = centerY + dy;

                if (cameraStick) {
                    float deltaX = x - lastTouchX;
                    float deltaY = y - lastTouchY;
                    if (action == MotionEvent.ACTION_MOVE && (deltaX != 0 || deltaY != 0)) {
                        float yaw = deltaX * 0.24f;
                        float pitch = -deltaY * 0.24f;
                        surface.queueEvent(() -> renderer.rotate(yaw, pitch));
                    }
                } else {
                    final float forward = -dy / limit;
                    final float strafe = dx / limit;
                    surface.queueEvent(() -> renderer.setMovement(forward, strafe));
                }
                lastTouchX = x;
                lastTouchY = y;
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                active = false;
                knobX = centerX;
                knobY = centerY;
                if (!cameraStick) {
                    surface.queueEvent(() -> renderer.setMovement(0f, 0f));
                }
                invalidate();
                return true;
            }
            return true;
        }
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