package com.terra.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.opengl.GLSurfaceView;

public final class MinecraftPreviewActivity extends Activity {
    private GLSurfaceView surface;
    private RubyDungVoxelRenderer renderer;
    private JoystickOverlay joysticks;
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

        joysticks = new JoystickOverlay();
        root.addView(joysticks, new FrameLayout.LayoutParams(-1, -1));

        TextView title = label("SOLARCRYSTAL 0.9.12  •  FIRST-PERSON VOXEL TEST");
        root.addView(title, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
        TextView help = label("LEFT: MOVE  •  RIGHT: CAMERA");
        root.addView(help, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(6, 4, 6, 4);
        actions.setBackgroundColor(0x99000000);
        Button breakButton = actionButton("BREAK");
        Button placeButton = actionButton("PLACE");
        Button blockButton = actionButton("BLOCK");
        actions.addView(breakButton);
        actions.addView(placeButton);
        actions.addView(blockButton);
        FrameLayout.LayoutParams actionsParams = new FrameLayout.LayoutParams(
            -2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        actionsParams.setMargins(0, 0, 0, dp(42));
        root.addView(actions, actionsParams);
        breakButton.setOnClickListener(v -> surface.queueEvent(() -> renderer.breakBlock()));
        placeButton.setOnClickListener(v -> surface.queueEvent(() -> renderer.placeBlock()));
        blockButton.setOnClickListener(v -> surface.queueEvent(() -> renderer.nextBlock()));

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
        button.setTextSize(11);
        button.setMinWidth(dp(72));
        button.setPadding(dp(8), dp(4), dp(8), dp(4));
        return button;
    }

    private TextView label(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(Color.WHITE);
        text.setTextSize(11);
        text.setPadding(dp(14), dp(10), dp(14), dp(10));
        text.setBackgroundColor(0x88000000);
        return text;
    }

    private final class JoystickOverlay extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int movePointer = -1;
        private int lookPointer = -1;
        private float moveX, moveY, lookX, lookY;
        private float leftCenterX, rightCenterX, centerY, radius;

        JoystickOverlay() {
            super(MinecraftPreviewActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        private void updateGeometry() {
            radius = dp(58);
            leftCenterX = Math.max(radius + dp(18), getWidth() * 0.16f);
            rightCenterX = Math.min(getWidth() - radius - dp(18), getWidth() * 0.84f);
            centerY = getHeight() - dp(122);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            updateGeometry();
            drawStick(canvas, leftCenterX, centerY, moveX, moveY, "MOVE");
            drawStick(canvas, rightCenterX, centerY, lookX, lookY, "LOOK");
        }

        private void drawStick(Canvas canvas, float cx, float cy,
                               float x, float y, String caption) {
            paint.setColor(0x50303030);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setColor(0x99FFFFFF);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            canvas.drawCircle(cx, cy, radius, paint);
            float knobX = cx + x * radius * 0.68f;
            float knobY = cy + y * radius * 0.68f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xBBDDEEFF);
            canvas.drawCircle(knobX, knobY, knobRadius(), paint);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(10));
            canvas.drawText(caption, cx, cy + radius + dp(17), paint);
        }

        private float knobRadius() { return radius * 0.30f; }

        private int stickAt(float x, float y) {
            updateGeometry();
            float dy = y - centerY;
            if (Math.hypot(x - leftCenterX, dy) <= radius * 1.35f) return 0;
            if (Math.hypot(x - rightCenterX, dy) <= radius * 1.35f) return 1;
            return -1;
        }

        private void setStick(int which, float x, float y) {
            float cx = which == 0 ? leftCenterX : rightCenterX;
            float dx = x - cx;
            float dy = y - centerY;
            float length = (float) Math.hypot(dx, dy);
            float max = radius;
            if (length > max) { dx *= max / length; dy *= max / length; }
            float nx = dx / max;
            float ny = dy / max;
            if (which == 0) {
                moveX = nx;
                moveY = ny;
                surface.queueEvent(() -> renderer.setMovement(-ny, nx));
            } else {
                lookX = nx;
                lookY = ny;
                surface.queueEvent(() -> renderer.setLookInput(nx, ny));
            }
            invalidate();
        }

        private void releaseStick(int which) {
            if (which == 0) {
                movePointer = -1;
                moveX = moveY = 0f;
                surface.queueEvent(() -> renderer.setMovement(0f, 0f));
            } else {
                lookPointer = -1;
                lookX = lookY = 0f;
                surface.queueEvent(() -> renderer.setLookInput(0f, 0f));
            }
            invalidate();
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            int index = event.getActionIndex();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                int which = stickAt(event.getX(index), event.getY(index));
                int pointer = event.getPointerId(index);
                if (which == 0 && movePointer == -1) {
                    movePointer = pointer;
                    setStick(0, event.getX(index), event.getY(index));
                    return true;
                }
                if (which == 1 && lookPointer == -1) {
                    lookPointer = pointer;
                    setStick(1, event.getX(index), event.getY(index));
                    return true;
                }
                return movePointer != -1 || lookPointer != -1;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pointer = event.getPointerId(i);
                    if (pointer == movePointer)
                        setStick(0, event.getX(i), event.getY(i));
                    else if (pointer == lookPointer)
                        setStick(1, event.getX(i), event.getY(i));
                }
                return movePointer != -1 || lookPointer != -1;
            }
            if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) {
                int pointer = event.getPointerId(index);
                if (pointer == movePointer) releaseStick(0);
                if (pointer == lookPointer) releaseStick(1);
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                if (movePointer != -1) releaseStick(0);
                if (lookPointer != -1) releaseStick(1);
                return true;
            }
            return movePointer != -1 || lookPointer != -1;
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