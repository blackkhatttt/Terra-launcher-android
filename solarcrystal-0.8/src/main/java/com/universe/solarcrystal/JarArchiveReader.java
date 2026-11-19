package com.universe.solarcrystal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Bounded in-memory reader for JAR/ZIP entries.
 * It reads archive data only; class definition and execution are separate.
 */
public final class JarArchiveReader {
    private static final int MAX_ARCHIVE_BYTES = 128 * 1024 * 1024;
    private static final int MAX_ENTRY_BYTES = 64 * 1024 * 1024;
    private static final int MAX_CLASS_BYTES = 16 * 1024 * 1024;
    private static final int MAX_CLASS_TOTAL_BYTES = 64 * 1024 * 1024;
    private static final int MAX_ENTRIES = 16384;
    private static final int BUFFER_SIZE = 8192;

    private JarArchiveReader() {}

    public static final class EntryInfo {
        public final String name;
        public final long size;
        public final boolean directory;

        private EntryInfo(String name, long size, boolean directory) {
            this.name = name;
            this.size = size;
            this.directory = directory;
        }
    }

    public static List<EntryInfo> listEntries(byte[] archive) throws IOException {
        requireArchive(archive);
        List<EntryInfo> result = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (result.size() >= MAX_ENTRIES) {
                    throw new IOException("JAR contains too many entries");
                }
                result.add(new EntryInfo(entry.getName(), entry.getSize(), entry.isDirectory()));
                drainEntry(zip, MAX_ENTRY_BYTES);
                zip.closeEntry();
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Reads only .class entries in one archive pass.
     * The returned map is bounded to 64 MiB of uncompressed class data.
     */
    public static Map<String, byte[]> readClassEntries(byte[] archive) throws IOException {
        requireArchive(archive);
        Map<String, byte[]> classes = new LinkedHashMap<>();
        int totalBytes = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (classes.size() >= MAX_ENTRIES) {
                    throw new IOException("JAR contains too many class entries");
                }
                String name = entry.getName();
                if (!entry.isDirectory() && name != null && name.endsWith(".class")) {
                    if (classes.containsKey(name)) {
                        throw new IOException("Duplicate JAR class entry: " + name);
                    }
                    byte[] bytes = readBounded(zip, MAX_CLASS_BYTES);
                    if (bytes.length > MAX_CLASS_TOTAL_BYTES - totalBytes) {
                        throw new IOException("Total uncompressed class data exceeds 64 MiB");
                    }
                    totalBytes += bytes.length;
                    classes.put(name, bytes);
                } else {
                    drainEntry(zip, MAX_ENTRY_BYTES);
                }
                zip.closeEntry();
            }
        }
        return Collections.unmodifiableMap(classes);
    }

    public static byte[] readEntry(byte[] archive, String wantedName) throws IOException {
        requireArchive(archive);
        if (wantedName == null || wantedName.length() == 0) {
            throw new IllegalArgumentException("Entry name is empty");
        }
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (wantedName.equals(entry.getName())) {
                    if (entry.isDirectory()) {
                        throw new IOException("Requested entry is a directory");
                    }
                    byte[] data = readBounded(zip, MAX_ENTRY_BYTES);
                    zip.closeEntry();
                    return data;
                }
                drainEntry(zip, MAX_ENTRY_BYTES);
                zip.closeEntry();
            }
        }
        throw new IOException("JAR entry not found: " + wantedName);
    }

    private static void requireArchive(byte[] archive) throws IOException {
        if (archive == null || archive.length == 0) {
            throw new IOException("JAR archive is empty");
        }
        if (archive.length > MAX_ARCHIVE_BYTES) {
            throw new IOException("JAR archive exceeds 128 MiB limit");
        }
    }

    private static void drainEntry(InputStream input, int limit) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (read > limit - total) {
                throw new IOException("JAR entry exceeds 64 MiB limit");
            }
            total += read;
        }
    }

    private static byte[] readBounded(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (read > limit - total) {
                throw new IOException("JAR entry exceeds configured size limit");
            }
            total += read;
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
