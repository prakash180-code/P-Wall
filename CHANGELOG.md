# Changelog

All notable changes to **P-Wall** are documented here.

## [1.0.0-SNAPSHOT] - Sprint 1 (Project Foundation)

### Added
- Gradle project foundation
  - Gradle 8.14.3 wrapper, AGP 8.13.2, Kotlin 2.2.21, Compose BOM 2025.06.01
  - Version catalog (`gradle/libs.versions.toml`), Kotlin DSL build scripts
  - `minSdk 29`, `targetSdk 36`, package `com.prakash.pwall`
- Material 3 theme with dark / light palettes and custom typography
- Clean Architecture project structure
  - `di/AppContainer` manual dependency container (constructor injection)
  - `data/model` immutable `WallpaperSettings` snapshot
  - `data/repository/SettingsRepository` (DataStore Preferences, transform-based API)
  - `data/storage/ImageStore` (copies picked image to app-private storage)
- Licensing abstraction
  - `license/LicenseManager` interface
  - `license/OfflineLicenseManager` (always activated, offline-first)
- Navigation
  - Navigation Compose `NavHost` with Home / Preview destinations
- Home screen
  - Material 3 cards: Select Image, Preview, Customize, Apply Wallpaper
  - Current wallpaper card with live clock overlay + selected-image thumbnail
  - Image picker: Android Photo Picker with SAF (`GetContent`) fallback
- Preview screen
  - Full-bleed image with live clock + date overlay (updates every second)
  - Shared `WallpaperPreview` + `ClockOverlay` composables
- Utilities
  - `ImageLoader` (sampled decode + in-memory `BitmapCache`)
  - `ClockTextFormatter` (12/24h, seconds, date formats)
  - Position preset → Compose alignment mapping
- Unit tests for the clock/date formatter and license manager
- Documentation: README, CHANGELOG, TODO

### Verified
- `assembleDebug` builds successfully (0 errors)
- Lint: 0 errors
- Unit tests: 10/10 pass
- Runtime smoke test on API 34 emulator:
  - App launches without crashes
  - Photo picker opens and image selection stores the image correctly
  - All settings persist to DataStore and restore after process restart
  - Home → Preview navigation renders image + live clock + date
  - Back navigation works

## [Unreleased] - Planned

See [TODO.md](TODO.md).
