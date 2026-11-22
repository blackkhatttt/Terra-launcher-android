package com.universe.solarcrystal;

import android.content.Context;
import android.net.Uri;
import java.util.Map;
import java.util.TreeMap;

/**
 * Converts the 0.9.3 static invocation audit into an Android compatibility plan.
 * This is a planning report only: it does not load or execute JAR classes.
 */
public final class RubyDungCompatibilityPlan {
    private RubyDungCompatibilityPlan() {}

    public static String inspect(Context context, Uri uri) throws Exception {
        String audit = RubyDungInvocationAudit.inspect(context, uri);
        Map<String, Integer> groups = new TreeMap<>();
        int total = 0;
        StringBuilder details = new StringBuilder();
        String[] lines = audit.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            int arrow = trimmed.indexOf(" -> ");
            int hash = trimmed.indexOf('#');
            if (arrow < 0 || hash < 0 || hash < arrow) continue;
            String call = trimmed.substring(arrow + 4);
            int callHash = call.indexOf('#');
            if (callHash <= 0) continue;
            String owner = call.substring(0, callHash);
            String category = category(owner);
            if (category == null) continue;
            total++;
            groups.put(category, groups.containsKey(category) ? groups.get(category) + 1 : 1);
            details.append("  [").append(category).append("] ").append(call).append("\n");
        }

        StringBuilder out = new StringBuilder();
        out.append("SOLARCRYSTAL 0.9.4 — RUBYDUNG COMPATIBILITY PLAN\n\n")
           .append("Source: actual invoke bytecodes from the 0.9.3 static audit\n")
           .append("Class execution: NOT STARTED\n")
           .append("Invocation callsites classified: ").append(total).append("\n\n")
           .append("COMPATIBILITY WORKSTREAMS\n")
           .append("1. WINDOW / DISPLAY — custom Android surface and lifecycle bridge. ")
           .append("LWJGL Display is not directly available on Android.\n")
           .append("2. OPENGL — implement a limited fixed-function-to-OpenGL ES adapter; ")
           .append("RubyDung's GL11 calls need individual coverage tests.\n")
           .append("3. INPUT — map Keyboard and Mouse actions to Android touch and hardware input.\n")
           .append("4. BUFFER / MATH — provide compatible direct buffers and required GLU math helpers.\n")
           .append("5. TEXTURES — replace AWT image APIs with Android Bitmap decoding and pixel access.\n")
           .append("6. UI — replace Swing dialogs with Android-native dialogs.\n")
           .append("7. AUDIO — no OpenAL calls means no OpenAL adapter is indicated by this scan; ")
           .append("recheck if another client JAR is selected.\n\n")
           .append("CALLSITE CLASSIFICATION\n");
        if (details.length() == 0) out.append("  No supported LWJGL/AWT/Swing invocation callsites found.\n");
        else out.append(details);
        out.append("\nCOUNTS BY WORKSTREAM\n");
        if (groups.isEmpty()) out.append("  none\n");
        else for (Map.Entry<String,Integer> e : groups.entrySet())
            out.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        out.append("\nNEXT ENGINE MILESTONE\n")
           .append("Build and test the Android display/input abstraction before attempting a client launch.\n")
           .append("This report is static analysis, not a compatibility guarantee.\n")
           .append("EXECUTION: NOT STARTED. No client bytecode was run.");
        return out.toString();
    }

    private static String category(String owner) {
        if (owner.startsWith("org/lwjgl/opengl/Display")) return "DISPLAY BACKEND";
        if (owner.startsWith("org/lwjgl/opengl/GL11")) return "FIXED-FUNCTION OPENGL";
        if (owner.startsWith("org/lwjgl/input/")) return "ANDROID INPUT BRIDGE";
        if (owner.startsWith("org/lwjgl/util/GLU")) return "GLU MATH HELPERS";
        if (owner.startsWith("org/lwjgl/BufferUtils")) return "DIRECT BUFFER SUPPORT";
        if (owner.startsWith("org/lwjgl/openal/")) return "AUDIO BACKEND";
        if (owner.startsWith("org/lwjgl/")) return "OTHER LWJGL API";
        if (owner.startsWith("java/awt/")) return "ANDROID BITMAP BRIDGE";
        if (owner.startsWith("javax/swing/")) return "ANDROID DIALOG BRIDGE";
        return null;
    }
}
