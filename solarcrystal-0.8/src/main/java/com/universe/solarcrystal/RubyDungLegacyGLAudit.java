package com.universe.solarcrystal;

import android.content.Context;
import android.net.Uri;
import java.util.Map;
import java.util.TreeMap;

/** Identifies RubyDung's legacy OpenGL features that need emulation or replacement. Static only. */
public final class RubyDungLegacyGLAudit {
    private RubyDungLegacyGLAudit() {}

    public static String inspect(Context context, Uri uri) throws Exception {
        String audit = RubyDungInvocationAudit.inspect(context, uri);
        Map<String, Integer> counts = new TreeMap<>();
        StringBuilder found = new StringBuilder();
        int total = 0, rewrite = 0, emulate = 0, basic = 0, unknown = 0;
        boolean inGl = false;
        for (String line : audit.split("\\n")) {
            if (line.startsWith("OPENGL INVOCATIONS")) { inGl = true; continue; }
            if (inGl && (line.startsWith("INPUT INVOCATIONS") || line.startsWith("AWT INVOCATIONS"))) break;
            if (!inGl) continue;
            String t = line.trim();
            int arrow = t.indexOf(" -> ");
            int hash = t.indexOf('#');
            if (arrow < 0 || hash < arrow) continue;
            String call = t.substring(arrow + 4);
            int callHash = call.indexOf('#');
            if (callHash < 0) continue;
            String owner = call.substring(0, callHash);
            if (!owner.startsWith("org/lwjgl/opengl/GL11")) continue;
            String method = call.substring(callHash + 1);
            String status = classify(method);
            if (status == null) { unknown++; status = "REVIEW REQUIRED"; }
            else if (status.startsWith("REWRITE")) rewrite++;
            else if (status.startsWith("EMULATE")) emulate++;
            else basic++;
            total++;
            counts.put(method, counts.containsKey(method) ? counts.get(method) + 1 : 1);
            found.append("  [").append(status).append("] ").append(method).append("\n");
        }

        StringBuilder out = new StringBuilder();
        out.append("SOLARCRYSTAL 0.9.5 — LEGACY OPENGL FEATURE AUDIT\n\n")
           .append("Source: RubyDung invoke bytecodes, parsed statically\n")
           .append("GL11 callsites classified: ").append(total).append("\n")
           .append("Straightforward GLES candidates: ").append(basic).append("\n")
           .append("Compatibility emulation required: ").append(emulate).append("\n")
           .append("Algorithm rewrite required: ").append(rewrite).append("\n")
           .append("Unclassified calls: ").append(unknown).append("\n\n")
           .append("CLASSIFICATION\n");
        if (found.length() == 0) out.append("  No GL11 invocation callsites were found in this archive.\n");
        else out.append(found);
        out.append("\nKNOWN HARD BLOCKERS\n")
           .append("• Display lists (glNewList/glEndList/glCallList): not available in OpenGL ES; replace with VBOs or cached geometry.\n")
           .append("• Selection mode (glSelectBuffer/glRenderMode/glInitNames/glPushName/glPopName): not available in OpenGL ES; replace with CPU ray picking.\n")
           .append("• Fixed-function matrix, lighting and fog calls: require shader/state emulation or a GLES 1.x path.\n")
           .append("• GLU texture mipmap helpers: replace with Android bitmap upload and GLES mipmap generation.\n")
           .append("\nNEXT IMPLEMENTATION ORDER\n")
           .append("1. Build a GL state/shader adapter for matrix and fixed-function state.\n")
           .append("2. Replace display lists with cached vertex/index buffers.\n")
           .append("3. Replace selection mode with CPU picking against the level geometry.\n")
           .append("4. Validate texture upload and mipmaps on an Android GLES context.\n")
           .append("\nThis report does not prove runtime reachability or successful rendering.\n")
           .append("EXECUTION: NOT STARTED. No client bytecode was run.");
        return out.toString();
    }

    private static String classify(String method) {
        if (method.startsWith("glSelectBuffer") || method.startsWith("glRenderMode")
                || method.startsWith("glInitNames") || method.startsWith("glPushName")
                || method.startsWith("glPopName"))
            return "REWRITE — CPU PICKING";
        if (method.startsWith("glNewList") || method.startsWith("glEndList")
                || method.startsWith("glCallList") || method.startsWith("glGenLists"))
            return "REWRITE — BUFFERED GEOMETRY";
        if (method.startsWith("glMatrixMode") || method.startsWith("glLoadIdentity")
                || method.startsWith("glTranslate") || method.startsWith("glRotate")
                || method.startsWith("glShadeModel") || method.startsWith("glFog")
                || method.startsWith("glColor") || method.startsWith("glVertex")
                || method.startsWith("glTexCoord"))
            return "EMULATE — FIXED FUNCTION STATE";
        if (method.startsWith("glDrawArrays") || method.startsWith("glClear")
                || method.startsWith("glEnable") || method.startsWith("glDisable")
                || method.startsWith("glDepthFunc") || method.startsWith("glBlendFunc")
                || method.startsWith("glBindTexture") || method.startsWith("glGenTextures")
                || method.startsWith("glTexParameteri") || method.startsWith("glGetError"))
            return "GLES CANDIDATE — VERIFY SIGNATURE";
        if (method.startsWith("glGetFloat") || method.startsWith("glGetInteger")
                || method.startsWith("glFogi") || method.startsWith("glFogf"))
            return "EMULATE — STATE QUERY / FOG";
        return null;
    }
}
