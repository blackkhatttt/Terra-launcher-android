package com.universe.solarcrystal;

import android.content.Context;
import android.net.Uri;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Bounded static readiness check for a user-provided RubyDung archive.
 * It never defines, initializes, or executes client bytecode.
 */
public final class RubyDungPreflight {
    private static final long MAX_ARCHIVE_BYTES = 128L * 1024L * 1024L;
    private static final int MAX_ENTRIES = 16384;
    private static final String ENTRY_CLASS = "com/mojang/rubydung/RubyDung.class";

    private RubyDungPreflight() {}

    public static String inspect(Context context, Uri uri) throws Exception {
        File archive = new File(context.getCacheDir(), "rubydung-preflight.jar");
        long copied = 0;
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(archive)) {
            if (input == null) throw new IllegalArgumentException("Android could not open the selected archive.");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                copied += count;
                if (copied > MAX_ARCHIVE_BYTES) throw new IllegalArgumentException("Archive exceeds 128 MiB.");
                output.write(buffer, 0, count);
            }
        }
        if (copied == 0) throw new IllegalArgumentException("Selected archive is empty.");

        int classes = 0;
        int resources = 0;
        int entriesSeen = 0;
        int highestMajor = -1;
        boolean entryClass = false;
        boolean terrain = false;
        boolean lwjgl = false;
        boolean opengl = false;
        boolean openal = false;
        boolean awt = false;
        boolean manifest = false;

        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry item = entries.nextElement();
                if (++entriesSeen > MAX_ENTRIES) {
                    throw new IllegalArgumentException("Archive contains too many entries (limit 16384).");
                }
                if (item.isDirectory()) continue;
                String name = item.getName();
                String lower = name.toLowerCase(Locale.ROOT);
                if (name.endsWith(".class")) {
                    classes++;
                    if (ENTRY_CLASS.equals(name)) entryClass = true;
                    try (InputStream input = zip.getInputStream(item)) {
                        byte[] head = new byte[8];
                        int got = input.read(head);
                        if (got == 8 && (head[0] & 255) == 0xca && (head[1] & 255) == 0xfe
                                && (head[2] & 255) == 0xba && (head[3] & 255) == 0xbe) {
                            int major = ((head[6] & 255) << 8) | (head[7] & 255);
                            if (major > highestMajor) highestMajor = major;
                        }
                        byte[] chunk = new byte[8192];
                        int read;
                        while ((read = input.read(chunk)) != -1) {
                            String sample = new String(chunk, 0, read, java.nio.charset.StandardCharsets.ISO_8859_1);
                            if (sample.contains("org/lwjgl/")) lwjgl = true;
                            if (sample.contains("org/lwjgl/opengl/")) opengl = true;
                            if (sample.contains("org/lwjgl/openal/")) openal = true;
                            if (sample.contains("java/awt/") || sample.contains("javax/swing/")) awt = true;
                        }
                    }
                } else {
                    resources++;
                    if (lower.equals("terrain.png") || lower.endsWith("/terrain.png")) terrain = true;
                    if (lower.equals("meta-inf/manifest.mf")) manifest = true;
                }
            }
        }

        boolean java6OrOlder = highestMajor >= 45 && highestMajor <= 50;
        boolean basicReady = entryClass && terrain && java6OrOlder;
        StringBuilder out = new StringBuilder();
        out.append("SOLARCRYSTAL 0.9.1 — RUBYDUNG PREFLIGHT\n\n");
        out.append("Archive size: ").append(copied).append(" bytes\n");
        out.append("Class files: ").append(classes).append("\n");
        out.append("Resources: ").append(resources).append("\n");
        out.append("Highest class major: ").append(highestMajor < 0 ? "unknown" : highestMajor).append("\n\n");
        out.append("RubyDung entry class: ").append(entryClass ? "FOUND" : "MISSING").append("\n");
        out.append("terrain.png: ").append(terrain ? "FOUND" : "MISSING").append("\n");
        out.append("Java bytecode range: ").append(java6OrOlder ? "45–50 (compatible era)" : "outside expected Java 1.1–6 range / unknown").append("\n");
        out.append("Manifest: ").append(manifest ? "present" : "not present").append("\n\n");
        out.append("LWJGL references: ").append(lwjgl ? "detected" : "not detected").append("\n");
        out.append("OpenGL references: ").append(opengl ? "detected" : "not detected").append("\n");
        out.append("OpenAL references: ").append(openal ? "detected" : "not detected").append("\n");
        out.append("AWT/Swing references: ").append(awt ? "detected" : "not detected").append("\n\n");
        out.append("ARCHIVE CHECK: ").append(basicReady ? "BASIC RUBYDUNG FILES FOUND" : "INCOMPLETE — CHECK MISSING ITEMS").append("\n");
        out.append("EXECUTION: NOT STARTED. This check does not prove JVM, LWJGL, OpenGL, input, or audio compatibility.");
        return out.toString();
    }
}
