# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.43.0] - 2026-10

Condensed summary of 0.11.0 – 0.43.0; individual releases are in the git history.

### Added

- Water tracking with a configurable daily target, overshoot bands, per-tap editing and its own diary row.
- Dishes: presets split into Dishes and Custom tabs, dishes logged by eaten portion, 40 seeded dish compositions.
- Fiber/sugar/salt totals (whole seed catalog filled and cross-checked against USDA SR Legacy), colored against daily norms.
- First-run onboarding, goal wizard, BMI tile (with an athletic-build toggle) and 7-day weight trend.
- Selectable Home progress style, day-part balance bar with carry and overshoot bands, macro overshoot tolerance.
- Coach messages with an ancient Greek flavour.
- Russian locale and dark theme.
- All settings mirrored into the database so backups and reinstalls keep them.
- APK build script (`build-apk.sh`).

### Changed

- History defaults to 30 days, with a weekly-bucketed year view; retention defaults to keeping everything.
- Weight trend is based on the first and last weigh-in.
- Compact UI density pass: unified spacing, form fields, search fields and settings grouped under headers.
- Floating action buttons replaced by regular buttons.

### Fixed

- A corrupted settings file is recreated instead of failing forever.
- Assorted History layout jumps and chart nudges.

## [0.10.0] - 2026-07-22

### Added

- User corrections to seeded catalog entries are now protected: once an entry is edited from search, future seed-catalog updates skip it instead of overwriting the fix (new editedByUser flag, database schema v8).

### Changed

- versionCode is now derived from the semantic version (MAJOR\*10000 + MINOR\*100 + PATCH), so it can never diverge from versionName; 0.10.0 maps to 1000, which still installs as a normal update over previous builds.

## [0.9.0] - 2026-07-22

### Added

- Food catalog entries (seeded, manually added, or cached from OpenFoodFacts) can now be edited straight from the search results via a pencil icon — useful for fixing wrong nutrition data. Corrections apply retroactively to past diary entries, since those store only grams and reference the food row.

### Changed

- Version numbering restarted at 0.x: the app is in alpha/beta and 1.0.0 is reserved for the production release. Earlier 1.x entries below predate this renumbering. The Android versionCode keeps increasing, so updates install normally.

## [1.8.2] - 2026-07-20

### Fixed

- The current day-part segment no longer shows "N kcal left" when the whole day's calorie goal is already exceeded (e.g. after overeating at lunch); the displayed remainder is now capped by what is left of the daily budget, and once the day is over budget the segment falls back to the factual consumed/target figures.

## [1.8.1] - 2026-07-20

### Changed

- Screen switches (bottom tabs and in-app navigation) now cross-fade in 200ms instead of the 700ms Navigation Compose default, which read as lag.

## [1.8.0] - 2026-07-20

### Added

- Preset items can now be edited in place: tapping an item in the preset editor opens the same quantity sheet used by the diary.

### Changed

- Preset editor items now use the same row card as the diary list (shared component), so both lists look identical and evolve together; the "Remove" text button is replaced by the same delete icon as in the diary.

## [1.7.0] - 2026-07-20

### Added

- Tapping the date label on the diary screen opens a calendar picker for jumping straight to a specific day; future days cannot be selected.

### Changed

- The Home tab button now always returns the diary to today, both when switching from another tab and when reselecting Home while viewing a past day.
