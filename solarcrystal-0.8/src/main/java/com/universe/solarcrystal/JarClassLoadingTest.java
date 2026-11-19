package com.universe.solarcrystal;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/** On-device integration test for JAR loading and the expanded integer VM. */
public final class JarClassLoadingTest {
    private static final String ASSET_NAME = "solarcrystal-smoke-test.jar";
    private static final String CLASS_NAME = "SolarCrystalJarFixture";
    private static final int MAX_JAR_BYTES = 8 * 1024 * 1024;

    private JarClassLoadingTest() {}

    public static String run(Context context) {
        StringBuilder output = new StringBuilder();
        try {
            byte[] jarBytes = readAsset(context);
            NativeBridge.nativeClearClassRegistry();
            ClassFileLoader.LoadReport report = ClassFileLoader.loadJar(jarBytes);
            output.append(report).append("\n\n");
            if (report.loaded != 1 || !report.errors.isEmpty()) {
                output.append("RESULT: FAIL — unexpected class registration count");
                return output.toString();
            }

            String classResult = NativeBridge.nativeResolveClass(CLASS_NAME);
            String methodResult = NativeBridge.nativeResolveMethod(
                    CLASS_NAME, "multiply", "(II)I");
            output.append("--- CLASS RESOLUTION ---\n").append(classResult)
                  .append("\n\n--- METHOD RESOLUTION ---\n").append(methodResult)
                  .append("\n\n");

            Map<String, byte[]> entries = JarArchiveReader.readClassEntries(jarBytes);
            byte[] fixture = entries.get("SolarCrystalJarFixture.class");
            if (fixture == null) {
                output.append("RESULT: FAIL — fixture class missing from JAR");
                return output.toString();
            }

            boolean multiply = check(fixture, "multiply", "(II)I",
                    new int[] {6, 7}, 42, output);
            boolean remainder = check(fixture, "remainder", "(II)I",
                    new int[] {17, 5}, 2, output);
            boolean bitwise = check(fixture, "bitwiseAnd", "(II)I",
                    new int[] {14, 6}, 6, output);
            boolean shift = check(fixture, "shiftLeft", "(II)I",
                    new int[] {3, 4}, 48, output);
            boolean branch = check(fixture, "absolute", "(I)I",
                    new int[] {-19}, 19, output);

            boolean classOk = classResult != null
                    && classResult.startsWith("SolarCrystal Class Resolver: OK");
            boolean methodOk = methodResult != null
                    && methodResult.startsWith("SolarCrystal Method Resolver: OK");
            output.append(classOk && methodOk && multiply && remainder
                    && bitwise && shift && branch
                    ? "RESULT: PASS — JAR loading, resolution and expanded VM tests passed"
                    : "RESULT: FAIL — one or more JAR/VM tests failed");
        } catch (IOException error) {
            output.append("RESULT: FAIL — ").append(error.getMessage());
        } catch (LinkageError error) {
            output.append("RESULT: FAIL — native library error: ").append(error);
        } catch (RuntimeException error) {
            output.append("RESULT: FAIL — ").append(error);
        }
        return output.toString();
    }

    private static boolean check(byte[] classBytes, String method,
            String descriptor, int[] arguments, int expected, StringBuilder output) {
        String result = NativeBridge.nativeExecuteClassMethodWithArgs(
                classBytes, method, descriptor, arguments);
        boolean passed = result != null
                && result.startsWith("SolarCrystal Class VM: OK")
                && result.contains("Return value: " + expected);
        output.append(method).append(": ").append(passed ? "PASS" : "FAIL")
              .append(" (expected ").append(expected).append(")\n");
        if (!passed) output.append(result == null ? "null" : result).append("\n");
        return passed;
    }

    private static byte[] readAsset(Context context) throws IOException {
        try (InputStream input = context.getAssets().open(ASSET_NAME);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (read > MAX_JAR_BYTES - total)
                    throw new IOException("Smoke-test JAR exceeds 8 MiB");
                total += read;
                output.write(buffer, 0, read);
            }
            if (total == 0) throw new IOException("Smoke-test JAR asset is empty");
            return output.toByteArray();
        }
    }
}
