# SolarCrystal 0.8.8c — Optimized voxel preview

This milestone polishes the experimental OpenGL ES 2.0 preview introduced in 0.8.8b.

Changes:
- Corrected cube visibility by disabling face culling for the generated mesh.
- Replaced hundreds of per-block draw calls with one combined static vertex/color mesh.
- Smoothed the generated terrain height profile.
- Rebuilt the trees with shorter trunks and compact, separated canopies.
- Enabled immersive fullscreen mode and reapplies it when the activity regains focus.
- Updated the preview label and Android version metadata to 0.8.8c.

Scope:
- This remains a generated graphics demonstration, not the Minecraft client.
- It does not load Minecraft assets, worlds, chunks, resource packs, or Java game classes.
- The 0.8.8c optimization reduces rendering submissions but does not implement chunk meshing or interactive world rendering.
