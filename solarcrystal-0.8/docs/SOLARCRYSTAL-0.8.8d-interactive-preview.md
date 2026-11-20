# SolarCrystal 0.8.8d — Interactive preview compatibility

This milestone adds basic touch interaction to the generated OpenGL ES 2.0 preview.

- One-finger drag rotates the camera around the fixed test scene.
- Two-finger pinch zooms in and out with bounded distance.
- Camera updates are queued onto the GLSurfaceView render thread.
- The preview remains immersive and displays the interaction hint.
- Android version metadata is set to versionCode 10 and versionName 0.8.8d after the baseline integration scripts run.

Scope:
- This is camera interaction for the generated graphics test only.
- It is not Minecraft player movement, world interaction, or a running Minecraft client.
- No Minecraft classes, assets, chunks, or resources are loaded.

Build verification is defined in `.github/workflows/build-solarcrystal-08.yml`; the Android integration applies 0.8.8d metadata after the legacy baseline integration.
