package com.terra.android;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.LinearGradient;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;

/** Block-world sunset backdrop for Universe Launcher. */
public final class SunsetVoxelDrawable extends Drawable {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Override public void draw(Canvas c) {
        Rect b = getBounds();
        float w = b.width(), h = b.height();
        if (w <= 0 || h <= 0) return;
        c.save();
        c.translate(b.left, b.top);
        p.setShader(new LinearGradient(0, 0, 0, h,
            new int[] { Color.rgb(31,35,91), Color.rgb(150,69,113),
                Color.rgb(255,145,83), Color.rgb(26,34,54) },
            new float[] { 0f, .43f, .72f, 1f }, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
        p.setColor(Color.rgb(255,211,120));
        float sun = Math.min(w,h) * .16f;
        c.drawRect(w*.68f, h*.30f, w*.68f+sun, h*.30f+sun, p);
        mountain(c, 0, h*.69f, w*.34f, h*.40f, Color.rgb(77,59,105));
        mountain(c, w*.22f, h*.72f, w*.37f, h*.34f, Color.rgb(89,62,101));
        mountain(c, w*.55f, h*.75f, w*.34f, h*.29f, Color.rgb(76,57,82));
        block(c, 0, h*.78f, w*.44f, h*.09f, Color.rgb(52,104,62));
        block(c, 0, h*.87f, w*.44f, h*.13f, Color.rgb(83,57,49));
        block(c, w*.12f, h*.72f, w*.30f, h*.07f, Color.rgb(64,122,65));
        block(c, w*.12f, h*.79f, w*.30f, h*.10f, Color.rgb(91,62,49));
        block(c, w*.57f, h*.82f, w*.43f, h*.08f, Color.rgb(52,104,62));
        block(c, w*.57f, h*.90f, w*.43f, h*.10f, Color.rgb(77,55,51));
        tree(c, w*.18f, h*.73f, Math.min(w,h)*.13f);
        tree(c, w*.88f, h*.76f, Math.min(w,h)*.11f);
        c.restore();
    }

    private void mountain(Canvas c, float x, float base, float width, float height, int color) {
        Path a = new Path();
        a.moveTo(x,base); a.lineTo(x+width*.42f,base-height);
        a.lineTo(x+width*.60f,base-height*.66f); a.lineTo(x+width,base); a.close();
        p.setColor(color); c.drawPath(a,p);
    }

    private void block(Canvas c, float x, float y, float w, float h, int color) {
        p.setColor(color); c.drawRect(x,y,x+w,y+h,p);
        p.setColor(Color.argb(35,255,255,255));
        c.drawRect(x,y,x+w,y+Math.max(2,h*.10f),p);
    }

    private void tree(Canvas c, float x, float y, float s) {
        p.setColor(Color.rgb(83,57,43)); c.drawRect(x+s*.42f,y-s*.45f,x+s*.58f,y,p);
        p.setColor(Color.rgb(37,91,57)); c.drawRect(x+s*.12f,y-s*.78f,x+s*.88f,y-s*.40f,p);
        c.drawRect(x+s*.25f,y-s,x+s*.75f,y-s*.70f,p);
    }

    @Override public void setAlpha(int a) { p.setAlpha(a); invalidateSelf(); }
    @Override public void setColorFilter(android.graphics.ColorFilter f) { p.setColorFilter(f); invalidateSelf(); }
    @Override public int getOpacity() { return android.graphics.PixelFormat.OPAQUE; }
}
