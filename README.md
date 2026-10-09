# VirtualEIS A54 — GitHub Actions build

## Build APK from your phone
1. Create a GitHub repository and upload the CONTENTS of this folder (not the outer ZIP folder).
2. Confirm `.github/workflows/build.yml` exists at repository root.
3. Open repository → Actions. Enable workflows if GitHub asks.
4. Select **Build VirtualEIS APK** → **Run workflow**.
5. Wait until the run completes successfully.
6. Open the completed run and download artifact **VirtualEIS-A54-debug-apk**.
7. Extract the downloaded artifact ZIP and install `app-debug.apk`.

## Important limitation
This baseline reads the Android TYPE_ROTATION_VECTOR sensor if available and records MP4 with CameraX.
It does NOT implement real-time frame stabilization/warping/cropping yet. Reading a virtual sensor alone
does not create EIS; this project is a buildable baseline for verifying the GitHub Actions pipeline.
