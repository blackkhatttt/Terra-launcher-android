package com.universe.solarcrystal;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/** Static preflight for user-selected historical client archives. Does not execute bytecode. */
public final class LegacyJarInspector {
    private LegacyJarInspector() {}

    public static String inspect(Context context, Uri uri) throws Exception {
        File copy = new File(context.getCacheDir(), "legacy-client-preflight.jar");
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(copy)) {
            if (in == null) throw new IllegalArgumentException("Android could not open this file.");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        }

        int classes = 0;
        int major = -1;
        boolean lwjgl = false, opengl = false, openal = false, awt = false;
        boolean rubyDung = false, minecraft = false;
        int resources = 0, nativeLibraries = 0;
        String mainClass = "(not declared)";
        StringBuilder report = new StringBuilder();

        try (JarFile jar = new JarFile(copy)) {
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                Attributes attrs = manifest.getMainAttributes();
                String value = attrs.getValue(Attributes.Name.MAIN_CLASS);
                if (value != null && !value.trim().isEmpty()) mainClass = value.trim();
            }
            Enumeration<java.util.jar.JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    classes++;
                    if (name.equals("com/mojang/rubydung/RubyDung.class")) rubyDung = true;
                    if (name.equals("net/minecraft/client/Minecraft.class")) minecraft = true;
                    try (InputStream in = jar.getInputStream(entry)) {
                        byte[] head = new byte[8];
                        int got = in.read(head);
                        if (got == 8 && (head[0] & 255) == 0xca && (head[1] & 255) == 0xfe
                                && (head[2] & 255) == 0xba && (head[3] & 255) == 0xbe) {
                            int found = ((head[6] & 255) << 8) | (head[7] & 255);
                            if (major < 0 || found > major) major = found;
                        }
                    }
                } else {
                    resources++;
                    String lower = name.toLowerCase(java.util.Locale.ROOT);
                    if (lower.endsWith(".so") || lower.endsWith(".dll") || lower.endsWith(".dylib")) nativeLibraries++;
                }
                if (name.contains("org/lwjgl/")) lwjgl = true;
                if (name.contains("org/lwjgl/opengl/")) opengl = true;
                if (name.contains("org/lwjgl/openal/")) openal = true;
                if (name.contains("java/awt/") || name.contains("javax/swing/")) awt = true;
            }
        }

        report.append("LEGACY CLIENT PREFLIGHT\n\n");
        report.append("Archive: ").append(copy.getName()).append("\n");
        report.append("Class files: ").append(classes).append("\n");
        report.append("Highest class major version: ").append(major < 0 ? "unknown" : major).append("\n");
        report.append("Manifest Main-Class: ").append(mainClass).append("\n");
        report.append("RubyDung entrypoint: ").append(rubyDung ? "found" : "not found").append("\n");
        report.append("Minecraft client entrypoint: ").append(minecraft ? "found" : "not found").append("\n");
        report.append("Resources: ").append(resources).append("\n");
        report.append("Bundled native libraries: ").append(nativeLibraries).append("\n\n");
        report.append("LWJGL references: ").append(lwjgl ? "detected" : "not detected").append("\n");
        report.append("OpenGL references: ").append(opengl ? "detected" : "not detected").append("\n");
        report.append("OpenAL references: ").append(openal ? "detected" : "not detected").append("\n");
        report.append("AWT/Swing references: ").append(awt ? "detected" : "not detected").append("\n\n");
        report.append("STATUS: ARCHIVE INSPECTED — GAME NOT EXECUTED.\n");
        report.append("SolarCrystal still needs a compatible legacy runtime and graphics/input bridge.");
        return report.toString();
    }
}
