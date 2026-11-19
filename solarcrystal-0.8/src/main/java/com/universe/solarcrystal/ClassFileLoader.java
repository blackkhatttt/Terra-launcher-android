package com.universe.solarcrystal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads class metadata from a JAR into SolarCrystal's native class registry.
 *
 * This is metadata registration only. It does not define JVM classes, verify
 * bytecode, initialize classes, resolve external dependencies, or execute code.
 */
public final class ClassFileLoader {
    private static final int MAX_CLASSES_PER_JAR = 2048;

    private ClassFileLoader() {}

    public static final class LoadReport {
        public final int loaded;
        public final int skipped;
        public final List<String> loadedClasses;
        public final List<String> errors;

        private LoadReport(int loaded, int skipped,
                           List<String> loadedClasses, List<String> errors) {
            this.loaded = loaded;
            this.skipped = skipped;
            this.loadedClasses = List.copyOf(loadedClasses);
            this.errors = List.copyOf(errors);
        }

        public boolean isSuccessful() {
            return errors.isEmpty() && loaded > 0;
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder();
            out.append("SolarCrystal JAR Class Loader: ")
               .append(errors.isEmpty() ? "OK" : "PARTIAL")
               .append('\n')
               .append("Classes registered: ").append(loaded).append('\n')
               .append("Entries skipped: ").append(skipped).append('\n');
            if (!loadedClasses.isEmpty()) {
                out.append("Loaded classes:\n");
                for (String name : loadedClasses) {
                    out.append("  ").append(name).append('\n');
                }
            }
            if (!errors.isEmpty()) {
                out.append("Errors:\n");
                for (String error : errors) {
                    out.append("  ").append(error).append('\n');
                }
            }
            if (loaded == 0 && errors.isEmpty()) {
                out.append("No loadable .class entries were found.");
            }
            return out.toString().trim();
        }
    }

    /**
     * Registers each eligible .class entry using NativeBridge.nativeLoadClass.
     * Already-registered classes are reported as errors; the registry is not
     * cleared implicitly because it may contain classes loaded by the caller.
     */
    public static LoadReport loadJar(byte[] jarBytes) throws IOException {
        Map<String, byte[]> classEntries = JarArchiveReader.readClassEntries(jarBytes);
        if (classEntries.size() > MAX_CLASSES_PER_JAR) {
            throw new IOException("JAR exceeds the limit of "
                    + MAX_CLASSES_PER_JAR + " class files");
        }

        int loaded = 0;
        int skipped = 0;
        List<String> loadedNames = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, byte[]> entry : classEntries.entrySet()) {
            String path = entry.getKey();
            if (isExcluded(path)) {
                skipped++;
                continue;
            }

            byte[] classBytes = entry.getValue();
            if (classBytes == null || classBytes.length == 0) {
                errors.add(path + ": empty class file");
                continue;
            }

            final String nativeResult;
            try {
                nativeResult = NativeBridge.nativeLoadClass(classBytes);
            } catch (UnsatisfiedLinkError error) {
                throw new IOException("SolarCrystal native library is unavailable", error);
            } catch (RuntimeException error) {
                errors.add(path + ": " + error.getMessage());
                continue;
            }

            if (nativeResult != null
                    && nativeResult.startsWith("SolarCrystal Class Loader: OK")) {
                loaded++;
                loadedNames.add(extractLoadedName(nativeResult, path));
            } else {
                errors.add(path + ": " + (nativeResult == null
                        ? "native loader returned null"
                        : nativeResult.replace('\n', ' ')));
            }
        }

        return new LoadReport(loaded, skipped, loadedNames, errors);
    }

    private static boolean isExcluded(String path) {
        return path.equals("module-info.class")
                || path.endsWith("/module-info.class")
                || path.startsWith("META-INF/versions/")
                || path.startsWith("/");
    }

    private static String extractLoadedName(String result, String fallback) {
        String marker = "Loaded: ";
        int start = result.indexOf(marker);
        if (start < 0) return fallback;
        start += marker.length();
        int end = result.indexOf('\n', start);
        return end < 0 ? result.substring(start) : result.substring(start, end);
    }
}
