package com.universe.solarcrystal;

import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Audits actual invoke bytecodes in class Code attributes. Does not execute client code. */
public final class RubyDungInvocationAudit {
    private static final long MAX_ARCHIVE_BYTES = 128L * 1024L * 1024L;
    private static final int MAX_ENTRIES = 16384, MAX_CLASS_BYTES = 8 * 1024 * 1024;
    private RubyDungInvocationAudit() {}

    public static String inspect(Context context, Uri uri) throws Exception {
        File archive = new File(context.getCacheDir(), "rubydung-invocation-audit.jar");
        long copied = 0;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(archive)) {
            if (in == null) throw new IllegalArgumentException("Could not open archive.");
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) != -1) {
                copied += n;
                if (copied > MAX_ARCHIVE_BYTES) throw new IllegalArgumentException("Archive exceeds 128 MiB.");
                out.write(b, 0, n);
            }
        }
        if (copied == 0) throw new IllegalArgumentException("Archive is empty.");

        Set<String> calls = new TreeSet<>(), lwjgl = new TreeSet<>(), gl = new TreeSet<>();
        Set<String> input = new TreeSet<>(), awt = new TreeSet<>(), swing = new TreeSet<>();
        Map<String, Integer> counts = new TreeMap<>();
        int entries = 0, classes = 0, major = -1, invocations = 0, malformed = 0;
        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> it = zip.entries();
            while (it.hasMoreElements()) {
                ZipEntry e = it.nextElement();
                if (++entries > MAX_ENTRIES) throw new IllegalArgumentException("Archive exceeds 16384 entries.");
                if (e.isDirectory() || !e.getName().endsWith(".class")) continue;
                if (e.getSize() > MAX_CLASS_BYTES) throw new IllegalArgumentException("Class exceeds 8 MiB.");
                byte[] bytes;
                try (InputStream in = zip.getInputStream(e)) { bytes = readBounded(in); }
                try {
                    Parsed p = parse(bytes); classes++; major = Math.max(major, p.major);
                    for (String call : p.calls) {
                        invocations++;
                        String owner = call.substring(0, call.indexOf('#'));
                        String line = e.getName().replace('/', '.').replace(".class", "") + " -> " + call;
                        if (owner.startsWith("org/lwjgl/")) {
                            lwjgl.add(line); calls.add(call); counts.put(owner, counts.containsKey(owner) ? counts.get(owner) + 1 : 1);
                            if (owner.startsWith("org/lwjgl/opengl/")) gl.add(line);
                            if (owner.startsWith("org/lwjgl/input/")) input.add(line);
                        } else if (owner.startsWith("java/awt/")) awt.add(line);
                        else if (owner.startsWith("javax/swing/")) swing.add(line);
                    }
                } catch (Exception ex) {
                    malformed++;
                }
            }
        }
        StringBuilder s = new StringBuilder();
        s.append("SOLARCRYSTAL 0.9.3 — RUBYDUNG INVOCATION AUDIT\n\n")
         .append("Archive bytes: ").append(copied).append("\nClass files parsed: ").append(classes)
         .append("\nHighest class major: ").append(major)
         .append("\nInvoke instructions found: ").append(invocations)
         .append("\nClass parse failures: ").append(malformed)
         .append("\n\nOnly method invocation bytecodes inside Code attributes are listed.\n")
         .append("A listed call may still be conditional and is not proof it executes at runtime.\n\n");
        section(s, "LWJGL INVOCATIONS", lwjgl);
        section(s, "OPENGL INVOCATIONS", gl);
        section(s, "INPUT INVOCATIONS", input);
        section(s, "AWT INVOCATIONS", awt);
        section(s, "SWING INVOCATIONS", swing);
        s.append("LWJGL OWNER CALL COUNTS\n");
        if (counts.isEmpty()) s.append("  none found\n");
        else for (Map.Entry<String,Integer> x : counts.entrySet()) s.append("  ").append(x.getKey()).append(": ").append(x.getValue()).append("\n");
        s.append("\nOPENAL INVOCATIONS: ").append(hasOpenAL(calls) ? "found" : "none found");
        s.append("\nEXECUTION: NOT STARTED. Static audit only; no client classes were loaded.");
        return s.toString();
    }

    private static boolean hasOpenAL(Set<String> calls) {
        for (String c : calls) if (c.startsWith("org/lwjgl/openal/")) return true;
        return false;
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
        int major; int[] tag, a, b; String[] utf; List<String> calls = new ArrayList<>();
        String str(int i) { return i > 0 && i < utf.length ? utf[i] : null; }
        String owner(int i) { return i > 0 && i < tag.length && tag[i] == 7 ? str(a[i]) : null; }
        String method(int index) {
            if (index <= 0 || index >= tag.length || (tag[index] != 10 && tag[index] != 11)) return null;
            String o = owner(a[index]); int nt = b[index];
            if (o == null || nt <= 0 || nt >= tag.length || tag[nt] != 12) return null;
            String name = str(a[nt]), desc = str(b[nt]);
            return name == null || desc == null ? null : o + "#" + name + desc;
        }
    }
    private static Parsed parse(byte[] bytes) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != 0xCAFEBABE) throw new IOException("Bad class magic");
            in.readUnsignedShort(); Parsed p = new Parsed(); p.major = in.readUnsignedShort();
            int n = in.readUnsignedShort(); p.tag = new int[n]; p.a = new int[n]; p.b = new int[n]; p.utf = new String[n];
            for (int i=1; i<n; i++) {
                int t = p.tag[i] = in.readUnsignedByte();
                switch (t) {
                    case 1: p.utf[i] = in.readUTF(); break;
                    case 3: case 4: in.skipBytes(4); break;
                    case 5: case 6: in.skipBytes(8); i++; break;
                    case 7: case 8: case 16: case 19: case 20: p.a[i] = in.readUnsignedShort(); break;
                    case 9: case 10: case 11: case 12: case 17: case 18:
                        p.a[i] = in.readUnsignedShort(); p.b[i] = in.readUnsignedShort(); break;
                    case 15: p.a[i] = in.readUnsignedByte(); p.b[i] = in.readUnsignedShort(); break;
                    default: throw new IOException("Unknown constant-pool tag " + t);
                }
            }
            in.readUnsignedShort(); in.readUnsignedShort(); in.readUnsignedShort();
            int interfaces = in.readUnsignedShort(); for (int i=0;i<interfaces;i++) in.readUnsignedShort();
            skipMembers(in, p, false);
            skipMembers(in, p, true);
            skipAttributes(in);
            return p;
        }
    }
    private static void skipMembers(DataInputStream in, Parsed p, boolean methods) throws IOException {
        int count = in.readUnsignedShort();
        for (int i=0;i<count;i++) {
            in.readUnsignedShort(); in.readUnsignedShort(); in.readUnsignedShort();
            int attrs = in.readUnsignedShort();
            for (int a=0;a<attrs;a++) {
                int nameIndex = in.readUnsignedShort(); long length = Integer.toUnsignedLong(in.readInt());
                if (length > 16L * 1024L * 1024L) throw new IOException("Attribute too large");
                byte[] data = new byte[(int)length]; in.readFully(data);
                if (methods && "Code".equals(p.str(nameIndex))) parseCode(data, p);
            }
        }
    }
    private static void skipAttributes(DataInputStream in) throws IOException {
        int count = in.readUnsignedShort();
        for (int i=0;i<count;i++) { in.readUnsignedShort(); long len=Integer.toUnsignedLong(in.readInt()); skipFully(in,len); }
    }
    private static void skipFully(DataInputStream in, long n) throws IOException {
        while (n > 0) { int skipped=in.skipBytes((int)Math.min(n, 1L<<20)); if (skipped<=0) { if(in.read()<0) throw new EOFException(); skipped=1; } n-=skipped; }
    }
    private static void parseCode(byte[] data, Parsed p) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            in.readUnsignedShort(); in.readUnsignedShort();
            long length = Integer.toUnsignedLong(in.readInt());
            if (length > data.length || length > MAX_CLASS_BYTES) throw new IOException("Invalid Code length");
            byte[] code = new byte[(int)length]; in.readFully(code);
            scanCode(code,p);
        }
    }
    private static void scanCode(byte[] c, Parsed p) throws IOException {
        int pc=0;
        while(pc<c.length) {
            int op=c[pc]&255;
            if(op==0xb6 || op==0xb7 || op==0xb8 || op==0xb9) {
                if(pc+2>=c.length) throw new IOException("Truncated invoke");
                int index=((c[pc+1]&255)<<8)|(c[pc+2]&255);
                String m=p.method(index); if(m!=null) p.calls.add(m);
            }
            int next=nextPc(c,pc,op);
            if(next<=pc || next>c.length) throw new IOException("Invalid bytecode instruction at "+pc);
            pc=next;
        }
    }
    private static int nextPc(byte[] c,int pc,int op) throws IOException {
        int p=pc+1;
        if(op==0xaa) {
            while((p&3)!=0) p++;
            if(p+12>c.length) throw new IOException("Bad tableswitch");
            int low=i4(c,p+4), high=i4(c,p+8);
            long end=(long)p+12L+4L*((long)high-low+1L);
            if(high<low || end>c.length) throw new IOException("Bad tableswitch bounds");
            return (int)end;
        }
        if(op==0xab) {
            while((p&3)!=0) p++;
            if(p+8>c.length) throw new IOException("Bad lookupswitch");
            int pairs=i4(c,p+4); long end=(long)p+8L+8L*pairs;
            if(pairs<0 || end>c.length) throw new IOException("Bad lookupswitch bounds");
            return (int)end;
        }
        if(op==0xc4) {
            if(p>=c.length) throw new IOException("Bad wide");
            return p+((c[p]&255)==0x84?5:3);
        }
        int len=1;
        if(op==0x10 || op==0x12 || (op>=0x15&&op<=0x19) || (op>=0x36&&op<=0x3a) || op==0xa9 || op==0xbc) len=2;
        else if(op==0x11 || op==0x13 || op==0x14 || (op>=0x99&&op<=0xa8) || (op>=0xb2&&op<=0xb8) || op==0xbb || op==0xbd || op==0xc0 || op==0xc1 || op==0xc6 || op==0xc7) len=3;
        else if(op==0xb9 || op==0xba || op==0xc8 || op==0xc9) len=5;
        else if(op==0xc5) len=4;
        else if(op==0xaa || op==0xab) throw new IOException("Switch parse failure");
        return pc+len;
    }
    private static int i4(byte[] b,int p) {
        return (b[p]&255)<<24 | (b[p+1]&255)<<16 | (b[p+2]&255)<<8 | (b[p+3]&255);
    }
    private static void section(StringBuilder s,String name,Set<String> values) {
        s.append(name).append(" (").append(values.size()).append(")\n");
        if(values.isEmpty()) s.append("  none found\n");
        else for(String v:values) s.append("  ").append(v).append("\n");
    }
}
