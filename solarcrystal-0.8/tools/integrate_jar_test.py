#!/usr/bin/env python3
from pathlib import Path

path = Path("terra-android/src/main/java/com/terra/android/MainActivity.java")
source = path.read_text(encoding="utf-8")

import_line = "import com.universe.solarcrystal.NativeBridge;"
if "import com.universe.solarcrystal.JarClassLoadingTest;" not in source:
    if import_line not in source:
        raise SystemExit("Could not find NativeBridge import in MainActivity.java")
    source = source.replace(
        import_line,
        import_line + "\\nimport com.universe.solarcrystal.JarClassLoadingTest;",
        1,
    )

button_anchor = "        testClassLoading.setOnClickListener(v -> runClassLoadingTest());"
button_code = """        testClassLoading.setOnClickListener(v -> runClassLoadingTest());

        Button testJarLoading = makeButton(
            "TEST SOLARCRYSTAL JAR CLASS LOADING",
            Color.rgb(34, 92, 66),
            Color.WHITE
        );
        LinearLayout.LayoutParams jarLoadingParams =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            );
        jarLoadingParams.topMargin = dp(8);
        page.addView(testJarLoading, jarLoadingParams);
        testJarLoading.setOnClickListener(v -> runJarClassLoadingTest());"""
if "runJarClassLoadingTest()" not in source:
    if button_anchor not in source:
        raise SystemExit("Could not find class-loading button anchor")
    source = source.replace(button_anchor, button_code, 1)

method_anchor = "    private void runClassReaderTest() {"
method_code = """    private void runJarClassLoadingTest() {
        showResult(
            "SolarCrystal 0.8 • JAR Class Loading Test",
            JarClassLoadingTest.run(this)
        );
    }

    private void runClassReaderTest() {"""
if "private void runJarClassLoadingTest()" not in source:
    if method_anchor not in source:
        raise SystemExit("Could not find class-reader method anchor")
    source = source.replace(method_anchor, method_code, 1)

path.write_text(source, encoding="utf-8")
