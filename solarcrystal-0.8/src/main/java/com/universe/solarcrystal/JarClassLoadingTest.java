package com.universe.solarcrystal;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * On-device integration test: reads a small JAR asset, registers its classes,
 * then asks the native metadata registry to resolve a known class and method.
 */
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
            output.append("--- CLASS RESOLUTION ---\n")
                  .append(classResult).append("\n\n");

            String methodResult = NativeBridge.nativeResolveMethod(
                    CLASS_NAME, "multiply", "(II)I");
            output.append("--- METHOD RESOLUTION ---\n")
                  .append(methodResult).append("\n\n");

            boolean classOk = classResult != null
                    && classResult.startsWith("SolarCrystal Class Resolver: OK");
            boolean methodOk = methodResult != null
                    && methodResult.startsWith("SolarCrystal Method Resolver: OK");

            output.append(classOk && methodOk
                    ? "RESULT: PASS — JAR entry loaded and metadata resolved"
                    : "RESULT: FAIL — class or method resolution failed");
        } catch (IOException error) {
            output.append("RESULT: FAIL — ").append(error.getMessage());
        } catch (LinkageError error) {
            output.append("RESULT: FAIL — native library error: ")
                  .append(error.toString());
        } catch (RuntimeException error) {
            output.append("RESULT: FAIL — ").append(error.toString());
        }
        return output.toString();
    }

    private static byte[] readAsset(Context context) throws IOException {
        try (InputStream input = context.getAssets().open(ASSET_NAME);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (read > MAX_JAR_BYTES - total) {
                    throw new IOException("Smoke-test JAR exceeds 8 MiB");
                }
                total += read;
                output.write(buffer, 0, read);
            }
            if (total == 0) {
                throw new IOException("Smoke-test JAR asset is empty");
            }
            return output.toByteArray();
        }
    }
}
