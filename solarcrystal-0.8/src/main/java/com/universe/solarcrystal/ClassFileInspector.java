package com.universe.solarcrystal;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bounded structural reader for JVM class files.
 * This inspects metadata only; it does not define or execute Java classes.
 */
public final class ClassFileInspector {
    private static final int MAX_CLASS_BYTES = 16 * 1024 * 1024;

    public static final class InspectionReport {
        public final String className;
        public final List<String> fields;
        public final List<String> methods;
        public final List<String> methodReferences;

        private InspectionReport(String name, List<String> f,
                List<String> m, List<String> refs) {
            className = name;
            fields = Collections.unmodifiableList(new ArrayList<>(f));
            methods = Collections.unmodifiableList(new ArrayList<>(m));
            methodReferences = Collections.unmodifiableList(new ArrayList<>(refs));
        }

        public boolean references(String owner, String name, String descriptor) {
            return methodReferences.contains(owner + "." + name + descriptor);
        }

        public boolean hasMethod(String name, String descriptor) {
            return methods.contains(name + descriptor);
        }

        @Override public String toString() {
            return "Class: " + className + "\nFields: " + fields.size()
                    + "\nMethods: " + methods.size()
                    + "\nMethod references: " + methodReferences.size();
        }
    }

    private ClassFileInspector() {}

    public static InspectionReport inspect(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length < 10 || bytes.length > MAX_CLASS_BYTES)
            throw new IOException("Class file is empty, truncated, or exceeds 16 MiB");

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        if (in.readInt() != 0xCAFEBABE) throw new IOException("Invalid class-file magic");
        in.readUnsignedShort();
        int major = in.readUnsignedShort();
        if (major < 45 || major > 70) throw new IOException("Unsupported class-file version: " + major);

        int count = in.readUnsignedShort();
        if (count < 1) throw new IOException("Invalid constant-pool count");
        int[] tags = new int[count];
        Object[] values = new Object[count];

        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            tags[i] = tag;
            switch (tag) {
                case 1: values[i] = in.readUTF(); break;
                case 3: case 4: in.readInt(); break;
                case 5: case 6:
                    in.readLong();
                    if (++i >= count) throw new IOException("Invalid two-slot constant-pool entry");
                    break;
                case 7: case 8: case 16: case 19: case 20:
                    values[i] = in.readUnsignedShort(); break;
                case 9: case 10: case 11: case 12: case 17: case 18:
                    values[i] = new int[] { in.readUnsignedShort(), in.readUnsignedShort() };
                    break;
                case 15:
                    values[i] = new int[] { in.readUnsignedByte(), in.readUnsignedShort() };
                    break;
                default: throw new IOException("Unsupported constant-pool tag " + tag);
            }
        }

        in.readUnsignedShort(); // access flags
        int thisClass = in.readUnsignedShort();
        in.readUnsignedShort(); // super
        String className = className(tags, values, thisClass);
        int interfaces = in.readUnsignedShort();
        skipFully(in, interfaces * 2L);

        List<String> fields = new ArrayList<>();
        int fieldCount = in.readUnsignedShort();
        for (int i = 0; i < fieldCount; i++) {
            in.readUnsignedShort();
            String name = utf8(tags, values, in.readUnsignedShort());
            String desc = utf8(tags, values, in.readUnsignedShort());
            fields.add(name + ":" + desc);
            skipAttributes(in, tags, values);
        }

        List<String> methods = new ArrayList<>();
        int methodCount = in.readUnsignedShort();
        for (int i = 0; i < methodCount; i++) {
            in.readUnsignedShort();
            String name = utf8(tags, values, in.readUnsignedShort());
            String desc = utf8(tags, values, in.readUnsignedShort());
            methods.add(name + desc);
            skipAttributes(in, tags, values);
        }

        skipAttributes(in, tags, values); // class attributes
        if (in.available() != 0) throw new IOException("Unexpected trailing class-file bytes");

        List<String> refs = new ArrayList<>();
        for (int i = 1; i < count; i++) {
            if (tags[i] != 10 && tags[i] != 11) continue;
            int[] member = (int[]) values[i];
            String owner = className(tags, values, member[0]);
            if (member[1] <= 0 || member[1] >= count || tags[member[1]] != 12)
                throw new IOException("Invalid NameAndType in method reference");
            int[] nt = (int[]) values[member[1]];
            refs.add(owner + "." + utf8(tags, values, nt[0])
                    + utf8(tags, values, nt[1]));
        }
        return new InspectionReport(className, fields, methods, refs);
    }

    private static String className(int[] tags, Object[] values, int index)
            throws IOException {
        if (index <= 0 || index >= tags.length || tags[index] != 7)
            throw new IOException("Invalid class constant-pool reference");
        return utf8(tags, values, (Integer) values[index]);
    }

    private static String utf8(int[] tags, Object[] values, int index)
            throws IOException {
        if (index <= 0 || index >= tags.length || tags[index] != 1)
            throw new IOException("Invalid UTF8 constant-pool reference");
        return (String) values[index];
    }

    private static void skipAttributes(DataInputStream in, int[] tags, Object[] values)
            throws IOException {
        int count = in.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            utf8(tags, values, in.readUnsignedShort());
            long length = ((long) in.readInt()) & 0xffffffffL;
            skipFully(in, length);
        }
    }

    private static void skipFully(DataInputStream in, long amount) throws IOException {
        if (amount < 0 || amount > in.available()) throw new EOFException("Attribute exceeds class-file bounds");
        long remaining = amount;
        while (remaining > 0) {
            int skipped = in.skipBytes((int) Math.min(remaining, 1_000_000L));
            if (skipped <= 0) throw new EOFException("Unexpected end of class file");
            remaining -= skipped;
        }
    }
}
