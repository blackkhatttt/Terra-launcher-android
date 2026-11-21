package com.universe.solarcrystal;

import android.content.Context;
import android.net.Uri;

/**
 * Compares two user-selected legacy archives using static metadata only.
 * It never loads or executes classes from either archive.
 */
public final class LegacyJarComparison {
    private LegacyJarComparison() {}

    public static String compare(Context context, Uri first, Uri second) throws Exception {
        String a = LegacyJarInspector.inspect(context, first);
        String b = LegacyJarInspector.inspect(context, second);

        int classesA = numberAfter(a, "Class files: ");
        int classesB = numberAfter(b, "Class files: ");
        int majorA = numberAfter(a, "Highest class major version: ");
        int majorB = numberAfter(b, "Highest class major version: ");
        boolean rubyA = containsLine(a, "RubyDung entrypoint: found");
        boolean rubyB = containsLine(b, "RubyDung entrypoint: found");
        boolean minecraftA = containsLine(a, "Minecraft client entrypoint: found");
        boolean minecraftB = containsLine(b, "Minecraft client entrypoint: found");
        boolean awtA = containsLine(a, "AWT/Swing references: detected");
        boolean awtB = containsLine(b, "AWT/Swing references: detected");
        boolean lwjglA = containsLine(a, "LWJGL references: detected");
        boolean lwjglB = containsLine(b, "LWJGL references: detected");
        boolean openalA = containsLine(a, "OpenAL references: detected");
        boolean openalB = containsLine(b, "OpenAL references: detected");
        int nativeA = numberAfter(a, "Bundled native libraries: ");
        int nativeB = numberAfter(b, "Bundled native libraries: ");

        StringBuilder out = new StringBuilder();
        out.append("SOLARCRYSTAL 0.9.0 — LEGACY CLIENT COMPARISON\n\n");
        appendClient(out, "CLIENT A", classesA, majorA, rubyA, minecraftA,
                awtA, lwjglA, openalA, nativeA);
        out.append("\n");
        appendClient(out, "CLIENT B", classesB, majorB, rubyB, minecraftB,
                awtB, lwjglB, openalB, nativeB);

        out.append("\nINITIAL TEST CANDIDATE: ");
        if (rubyA != rubyB) {
            out.append(rubyA ? "CLIENT A (RubyDung entrypoint detected)" :
                    "CLIENT B (RubyDung entrypoint detected)");
        } else if (minecraftA != minecraftB) {
            out.append(minecraftA ? "CLIENT A (Minecraft entrypoint detected)" :
                    "CLIENT B (Minecraft entrypoint detected)");
        } else {
            int scoreA = compatibilityScore(classesA, awtA, lwjglA, openalA, nativeA);
            int scoreB = compatibilityScore(classesB, awtB, lwjglB, openalB, nativeB);
            if (scoreA == scoreB) out.append("NO CLEAR WINNER — test both separately");
            else out.append(scoreA < scoreB ? "CLIENT A (simpler static profile)" :
                    "CLIENT B (simpler static profile)");
        }

        out.append("\n\nIMPORTANT: This is a static comparison, not a launch test.");
        out.append("\nNo client bytecode was executed. Native graphics, input, audio,");
        out.append("\nand Java compatibility still need separate runtime work.");
        return out.toString();
    }

    private static void appendClient(StringBuilder out, String label, int classes, int major,
            boolean ruby, boolean minecraft, boolean awt, boolean lwjgl,
            boolean openal, int nativeCount) {
        out.append(label).append("\n");
        out.append("Class files: ").append(classes < 0 ? "unknown" : classes).append("\n");
        out.append("Highest class major: ").append(major < 0 ? "unknown" : major).append("\n");
        out.append("RubyDung entrypoint: ").append(ruby ? "yes" : "no").append("\n");
        out.append("Minecraft entrypoint: ").append(minecraft ? "yes" : "no").append("\n");
        out.append("LWJGL: ").append(lwjgl ? "detected" : "not detected").append("\n");
        out.append("OpenAL: ").append(openal ? "detected" : "not detected").append("\n");
        out.append("AWT/Swing: ").append(awt ? "detected" : "not detected").append("\n");
        out.append("Bundled native libraries: ").append(nativeCount < 0 ? "unknown" : nativeCount).append("\n");
    }

    private static int compatibilityScore(int classes, boolean awt, boolean lwjgl,
            boolean openal, int nativeCount) {
        if (classes < 0) return Integer.MAX_VALUE;
        return classes + (awt ? 100 : 0) + (lwjgl ? 15 : 0)
                + (openal ? 20 : 0) + Math.max(0, nativeCount) * 30;
    }

    private static int numberAfter(String report, String prefix) {
        for (String line : report.split("\\n")) {
            if (!line.startsWith(prefix)) continue;
            try { return Integer.parseInt(line.substring(prefix.length()).trim()); }
            catch (NumberFormatException ignored) { return -1; }
        }
        return -1;
    }

    private static boolean containsLine(String report, String expected) {
        for (String line : report.split("\\n")) if (line.trim().equals(expected)) return true;
        return false;
    }
}
