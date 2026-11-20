#!/usr/bin/env python3
from pathlib import Path
root=Path.cwd()
base=Path(__file__).resolve().parent
android=root/"terra-android/src/main"
java_dir=android/"java/com/terra/android"
java_dir.mkdir(parents=True,exist_ok=True)
for name in ("MinecraftPreviewRenderer.java","MinecraftPreviewActivity.java"):
    source=base.parent/"android"/name
    (java_dir/name).write_text(source.read_text(encoding="utf-8"),encoding="utf-8")
activity=java_dir/"MainActivity.java"
text=activity.read_text(encoding="utf-8")
anchor='        status = new TextView(this);'
button='''        Button minecraftPreview = makeButton(
            "OPEN FIRST MINECRAFT GRAPHICS",
            Color.rgb(35, 105, 155),
            Color.WHITE
        );
        LinearLayout.LayoutParams previewParams =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            );
        previewParams.topMargin = dp(14);
        page.addView(minecraftPreview, previewParams);
        minecraftPreview.setOnClickListener(v ->
            startActivity(new android.content.Intent(this, MinecraftPreviewActivity.class))
        );

'''
if "OPEN FIRST MINECRAFT GRAPHICS" not in text:
    if anchor not in text: raise SystemExit("MainActivity insertion point not found")
    text=text.replace(anchor,button+anchor,1)
text=text.replace("VERSION 0.7","VERSION 0.8.8b").replace("SOLARCRYSTAL 0.7","SOLARCRYSTAL 0.8.8b")
activity.write_text(text,encoding="utf-8")
manifest=android/"AndroidManifest.xml"
m=manifest.read_text(encoding="utf-8")
if "MinecraftPreviewActivity" not in m:
    marker='        <activity\n            android:name=".MainActivity"'
    entry='''        <activity
            android:name=".MinecraftPreviewActivity"
            android:exported="false"
            android:screenOrientation="landscape" />
'''
    if marker not in m: raise SystemExit("Manifest MainActivity declaration not found")
    m=m.replace(marker,entry+marker,1)
if 'android:glEsVersion="0x00020000"' not in m:
    m=m.replace('<application','<uses-feature android:glEsVersion="0x00020000" android:required="true" />\n    <application',1)
manifest.write_text(m,encoding="utf-8")
gradle=root/"terra-android/build.gradle"
g=gradle.read_text(encoding="utf-8").replace("versionCode 7","versionCode 8").replace("versionName '0.7.0'","versionName '0.8.8b'")
gradle.write_text(g,encoding="utf-8")
print("SolarCrystal 0.8.8b OpenGL ES 2.0 voxel preview integrated.")
