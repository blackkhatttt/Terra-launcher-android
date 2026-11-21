package com.universe.solarcrystal;

import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Static class-file constant-pool inventory. Never loads or executes client classes. */
public final class RubyDungApiInventory {
    private static final long MAX_ARCHIVE_BYTES = 128L * 1024L * 1024L;
    private static final int MAX_ENTRIES = 16384;
    private static final int MAX_CLASS_BYTES = 8 * 1024 * 1024;
    private RubyDungApiInventory() {}

    public static String inspect(Context context, Uri uri) throws Exception {
        File archive = new File(context.getCacheDir(), "rubydung-api-inventory.jar");
        long copied = 0;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(archive)) {
            if (in == null) throw new IllegalArgumentException("Could not open archive.");
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) {
                copied += n;
                if (copied > MAX_ARCHIVE_BYTES) throw new IllegalArgumentException("Archive exceeds 128 MiB.");
                out.write(buffer, 0, n);
            }
        }
        if (copied == 0) throw new IllegalArgumentException("Archive is empty.");

        Set<String> lwjglClasses = new TreeSet<>(), lwjglMembers = new TreeSet<>();
        Set<String> gl = new TreeSet<>(), input = new TreeSet<>(), glu = new TreeSet<>();
        Set<String> awt = new TreeSet<>(), swing = new TreeSet<>(), callers = new TreeSet<>();
        int parsed = 0, seen = 0, major = -1;
        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (++seen > MAX_ENTRIES) throw new IllegalArgumentException("Archive exceeds 16384 entries.");
                if (e.isDirectory() || !e.getName().endsWith(".class")) continue;
                if (e.getSize() > MAX_CLASS_BYTES) throw new IllegalArgumentException("Class exceeds 8 MiB: " + e.getName());
                byte[] bytes;
                try (InputStream in = zip.getInputStream(e)) { bytes = readBounded(in); }
                Parsed p = parse(bytes); parsed++; major = Math.max(major, p.major);
                for (String c : p.classes) if (c.startsWith("org/lwjgl/")) lwjglClasses.add(c.replace('/', '.'));
                for (String m : p.members) {
                    if (m.startsWith("org/lwjgl/")) {
                        String v = m.replace('/', '.'); lwjglMembers.add(v);
                        if (m.startsWith("org/lwjgl/opengl/GL11.")) gl.add(v);
                        if (m.startsWith("org/lwjgl/input/")) input.add(v);
                        if (m.startsWith("org/lwjgl/util/glu/")) glu.add(v);
                        callers.add(e.getName().replace('/', '.').replace(".class", ""));
                    } else if (m.startsWith("java/awt/")) awt.add(m.replace('/', '.'));
                    else if (m.startsWith("javax/swing/")) swing.add(m.replace('/', '.'));
                }
            }
        }
        StringBuilder s = new StringBuilder();
        s.append("SOLARCRYSTAL 0.9.2 — RUBYDUNG API INVENTORY\n\n")
         .append("Archive bytes: ").append(copied).append("\nClass files parsed: ").append(parsed)
         .append("\nHighest class major: ").append(major)
         .append("\n\nReferences are read from class-file constant pools.\n")
         .append("They are symbolic references, not proof every path executes.\n\n");
        section(s, "LWJGL CLASSES", lwjglClasses); section(s, "LWJGL MEMBERS", lwjglMembers);
        section(s, "OPENGL GL11 MEMBERS", gl); section(s, "INPUT MEMBERS", input);
        section(s, "GLU MEMBERS", glu); section(s, "JAVA AWT MEMBERS", awt);
        section(s, "SWING MEMBERS", swing); section(s, "CLASSES CALLING LWJGL", callers);
        s.append("\nOPENAL: no OpenAL member references found.\n")
         .append("EXECUTION: NOT STARTED. Runtime compatibility is not provided by this report.");
        return s.toString();
    }

    private static byte[] readBounded(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[8192]; int n;
        while ((n = in.read(b)) != -1) {
            if (out.size() + n > MAX_CLASS_BYTES) throw new IllegalArgumentException("Class entry too large.");
            out.write(b, 0, n);
        }
        return out.toByteArray();
    }

    private static final class Parsed {
        int major; String[] utf; int[] tag, a, b;
        Set<String> classes = new TreeSet<>(), members = new TreeSet<>();
        String str(int i) { return i > 0 && i < utf.length ? utf[i] : null; }
        String className(int i) { return i > 0 && i < tag.length && tag[i] == 7 ? str(a[i]) : null; }
    }

    private static Parsed parse(byte[] bytes) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != 0xCAFEBABE) throw new IllegalArgumentException("Invalid class magic.");
            in.readUnsignedShort(); Parsed p = new Parsed(); p.major = in.readUnsignedShort();
            int count = in.readUnsignedShort(); p.utf = new String[count];
            p.tag = new int[count]; p.a = new int[count]; p.b = new int[count];
            for (int i = 1; i < count; i++) {
                int t = p.tag[i] = in.readUnsignedByte();
                switch (t) {
                    case 1: {
                        int len = in.readUnsignedShort(); byte[] data = new byte[len]; in.readFully(data);
                        p.utf[i] = new String(data, StandardCharsets.UTF_8); break;
                    }
                    case 3: case 4: in.skipBytes(4); break;
                    case 5: case 6: in.skipBytes(8); i++; break;
                    case 7: case 8: case 16: case 19: case 20: p.a[i] = in.readUnsignedShort(); break;
                    case 9: case 10: case 11: case 12: case 17: case 18:
                        p.a[i] = in.readUnsignedShort(); p.b[i] = in.readUnsignedShort(); break;
                    case 15: p.a[i] = in.readUnsignedByte(); p.b[i] = in.readUnsignedShort(); break;
                    default: throw new IllegalArgumentException("Unknown constant-pool tag " + t);
                }
            }
            for (int i = 1; i < count; i++) {
                if (p.tag[i] == 7) {
                    String c = p.str(p.a[i]); if (c != null) p.classes.add(c);
                } else if (p.tag[i] == 9 || p.tag[i] == 10 || p.tag[i] == 11) {
                    String owner = p.className(p.a[i]); int nt = p.b[i];
                    if (owner != null && nt > 0 && nt < count && p.tag[nt] == 12) {
                        String name = p.str(p.a[nt]), desc = p.str(p.b[nt]);
                        if (name != null && desc != null) p.members.add(owner + "." + name + " " + desc);
                    }
                }
            }
            return p;
        }
    }

    private static void section(StringBuilder s, String title, Set<String> values) {
        s.append(title).append(" (").append(values.size()).append(")\n");
        if (values.isEmpty()) s.append("  none found\n");
        else for (String v : values) s.append("  ").append(v).append("\n");
    }
}
