# Diaita

[![CI](https://github.com/publius-github/diaita/actions/workflows/ci.yml/badge.svg)](https://github.com/publius-github/diaita/actions/workflows/ci.yml)

A simple, offline-first calorie, macro and weight tracker for Android. Log meals in
seconds, reuse presets, and keep an eye on your pace through the day — no account,
no cloud, all data stays on the device.

Built with Kotlin and Jetpack Compose. The application id is
`app.diaita`; "Diaita" is the display name.

## Features

**Food diary**
- Log foods with per-meal slots (breakfast / lunch / dinner / snack) and day-part
  grouping (morning / day / evening with configurable boundaries).
- Running daily totals with a calorie progress ring and optional macro (P/F/C) tracking.
- Edit quantities in place, delete with undo, log today's weight right from the Home screen.

**Adding food**
- Search the local food catalog (seeded with common foods) with instant results.
- Online search via [OpenFoodFacts](https://openfoodfacts.org) (optional, can be turned
  off) — results are cached locally, so previously found foods work offline.
- Barcode scanner for packaged products.
- Manual entry for anything else, plus a recent-foods quick row.

**Presets**
- Save recurring meals as presets and log them with one tap.
- Presets show as a single collapsed row in the diary and can be edited later.

**Goals and history**
- Daily calorie and macro targets, optional weight target.
- Calorie wizard to estimate daily needs.
- 30-day history chart with over-goal highlighting and per-day drill-down.
- Weight tracking with a trend chart and target line.
- Entries older than 30 days are folded into monthly averages automatically.

**Data ownership**
- Full backup and restore to a single file.
- CSV export of the diary for spreadsheets.
- Optional daily reminders to log food and weight.

## Tech stack

| Area | Choice |
| --- | --- |
| Language / UI | Kotlin, Jetpack Compose (Material 3) |
| Architecture | MVVM, single Activity, manual DI (`AppContainer`) |
| Persistence | Room (exported schemas, tested migrations), DataStore Preferences |
| Networking | Retrofit + OkHttp + kotlinx.serialization (OpenFoodFacts only) |
| Background work | WorkManager (reminders), coroutine-based retention maintenance |
| Charts | Hand-rolled Compose `Canvas` charts (no charting library) |
| Quality | ktlint, unit tests, Room migration tests, GitHub Actions CI |

## Requirements

- Android Studio (any recent version) with the Android SDK, or just a JDK 17+ for
  command-line builds.
- SDK Platform 36 (Android Studio installs it automatically on first Gradle sync).
- Device or emulator running Android 8.0+ (`minSdk = 26`).

Gradle, AGP and Kotlin versions are pinned by the Gradle wrapper and the version
catalog (`gradle/libs.versions.toml`) — no global installs needed.

## Build and run

### Android Studio

1. File → Open, select the project folder, wait for Gradle sync.
2. Pick a device or emulator and press Run.

### Command line

```bash
# macOS / Linux / WSL
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Install on a phone via adb

1. Enable Developer Options (tap Build number 7 times) and USB debugging.
2. Connect via USB and confirm the device shows up in `adb devices`.
3. `adb install -r app/build/outputs/apk/debug/app-debug.apk`

## Release builds

Release builds are minified (R8) and signed with a local keystore that is never
committed. To produce a signed release APK, add to `local.properties`:

```properties
RELEASE_STORE_FILE=<path to your .jks, relative to the project root>
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

Then run `./gradlew assembleRelease`. Without these properties the release build
still compiles but produces an unsigned APK (this is what CI does).

Bump `versionCode` in `app/build.gradle.kts` before sharing a new APK — Android
refuses to update an app whose `versionCode` did not increase.

## Testing and checks

```bash
./gradlew ktlintCheck        # code style
./gradlew testDebugUnitTest  # unit tests
./gradlew connectedAndroidTest  # Room migration tests (needs a device/emulator)
```

CI (GitHub Actions) runs ktlint, assembles debug and release, and runs unit tests
on every push and pull request.

## Project structure

```
app/src/main/java/app/diaita/
├── data/          # Room entities/DAOs, repositories, OpenFoodFacts client,
│                  # backup, CSV export, reminders, retention, settings
├── ui/            # One package per screen (diary, addfood, presets, goals,
│                  # history, daydetail, settings, about) + shared components
└── util/          # Small helpers
app/schemas/       # Exported Room schemas (fixtures for migration tests)
the project notes       # Development log: architecture decisions and build history
```

## Privacy

The app makes network requests only to the OpenFoodFacts API, and only when online
search is enabled. There is no analytics, no accounts and no data collection —
everything is stored in a local database on the device.

## License

[MIT](LICENSE)
