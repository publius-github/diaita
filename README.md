# Diaita

A simple offline-first calorie and macro tracker for Android: manual food entry, local + OpenFoodFacts search, meal presets (with editing), meal slots (breakfast/lunch/dinner/snack), daily goals, weight tracking, a 30-day history chart with day drill-down, monthly averages, and CSV export.

The Android app id remains `app.diaita`; "Diaita" is the display name and launcher label.

See `the project notes` for the full architecture and phase-by-phase build log.

## Requirements

- **Android Studio** (any recent version — developed against a 2026 release) with the Android SDK.
- **JDK 17+** (bundled with Android Studio, no separate install needed).
- SDK Platform **36** and Build-Tools **36.1.0** (Android Studio's SDK Manager will prompt to install these automatically on first Gradle sync if missing).

The project pins its own Gradle/AGP/Kotlin versions via the Gradle wrapper and version catalog (`gradle/libs.versions.toml`), so no separate global installs of Gradle or Kotlin are required.

## Build & run from Android Studio (recommended)

1. **File → Open**, select this project's folder.
2. Wait for Gradle sync to finish (first sync downloads dependencies — can take a few minutes).
3. Pick a target device from the device dropdown (create an emulator via **Device Manager** if needed — see `the project notes` Phase 0 for setup notes).
4. Click **Run ▶**. This builds the debug APK and installs it on the selected device automatically.

If a build ever fails with a stale-looking error after pulling new code, do a clean rebuild before assuming it's broken: **Build → Clean Project**, then **Build → Assemble Project**, then **Run** again.

## Build from the command line

The project includes a Gradle wrapper, so no local Gradle install is required.

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux / WSL
./gradlew assembleDebug
```

The output APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Install the APK manually (no Play Store)

This app is debug-only, sideloaded via `adb` — it isn't published anywhere.

1. On the phone: **Settings → About phone** → tap **Build number** 7 times to unlock Developer Options.
2. **Settings → Developer Options** → enable **USB debugging**.
3. Connect the phone via USB and accept the "Allow USB debugging?" prompt.
4. Verify it's detected: `adb devices` should list it as `device` (not `unauthorized`).
5. Install the APK:

   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

   (On Windows, use backslashes if running from `cmd`/PowerShell: `app\build\outputs\apk\debug\app-debug.apk`.)

## Notes

- No release/signed build is configured — this is a personal-use app distributed as a plain debug APK.
- The local Room database resets on schema changes (`fallbackToDestructiveMigration()`), which is expected during active development — see `the project notes` for details.
