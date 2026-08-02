# Changelog

All notable changes to **P-Wall** are documented here.

## [1.0.0] - Sprint 3 (Production Ready)

### Added
- Release build pipeline
  - R8 minification + resource shrinking (`assembleRelease`: 28.8 MB debug →
    2.1 MB release)
  - `proguard-rules.pro` keeps the wallpaper service binder glue intact
  - Release signed with the debug key as a placeholder; swap in a real
    signing config before publishing
- Home "Current Wallpaper" card is now a true live preview: the clock + date
  overlay renders over the selected image (was a static crop)
- Safe wallpaper-picker launcher (`launchWallpaperPicker`) that swallows the
  rare no-handler case
- Image selection hardening: `ImageStore` validates the copied file decodes
  as a non-empty image and cleans up partial files on failure

### Changed
- `PWallWallpaperService` now decodes the wallpaper image off the main thread
  and survives transient canvas errors (best-effort frames, never kills the
  render thread)
- Removed dead `positionPresetToAlignment` helper
- Dark / light theme verified end-to-end (system night-mode toggle)

### Verified
- `assembleDebug` + `assembleRelease` build successfully
- Lint: 0 errors
- Unit tests: 16/16 pass
- Runtime smoke test on a physical device (API 33) with the **release** APK:
  - App launches; Home live-preview card shows clock + date over the image
  - Customize screen renders all sections; setting changes apply live and
    persist to DataStore across process restart (release)
  - Apply Wallpaper → system live wallpaper picker → P-Wall set as active
    wallpaper for home + lock screen
  - `pwall-renderer` thread runs and stays alive; no crashes
  - Dark mode renders correctly

## [1.0.0-SNAPSHOT] - Sprint 2 (Wallpaper Engine)

### Added
- Live wallpaper engine
  - `PWallWallpaperService`: `WallpaperService` with its own `Engine`, 1-second
    aligned redraw loop, pauses rendering when the wallpaper is not visible,
    loads the selected image, and collects settings from DataStore
  - `WallpaperRenderer`: pure Canvas renderer for background + clock block
    (shared logic between engine and unit tests)
  - `wallpaper.xml` metadata + manifest service declaration
    (BIND_WALLPAPER, intent filter, meta-data)
- Customization screen (`ui/customize`)
  - `CustomizeScreen`: preview with drag-to-position + all setting sections
  - `CustomizeViewModel`: every setter persists immediately via
    `SettingsRepository`
  - `ColorPicker` composable (hue/saturation/brightness sliders)
  - Clock overlay rewrite: edge padding + `extraOffset` override for drag
- Navigation: Home → Customize, Preview → Customize
- Apply Wallpaper flow: `wallpaperPickerIntent` launches the Android live
  wallpaper picker from Home / Preview / Customize
- Unit tests for `WallpaperRenderer` positioning + background transform
  (pure-logic helpers, no Android types)

### Changed
- `ClockOverlay` / `WallpaperPreview`: shared position logic matches the
  wallpaper engine (presets anchor to edges, CUSTOM centers on a fraction)

### Verified
- `assembleDebug` builds successfully (0 errors)
- Lint: 0 errors (15 warnings are intentional: pinned versions + v26 mipmap)
- Unit tests: 16/16 pass
- Runtime smoke test on a physical device (API 33):
  - Customize screen renders all sections without crashes
  - Position presets and background modes apply and persist
  - Drag-to-custom-position persists custom fractions
  - All changes verified in the DataStore preferences file
  - Apply Live Wallpaper opens the system live wallpaper picker (P-Wall listed)
  - Setting P-Wall makes `PWallWallpaperService` the active wallpaper
  - `pwall-renderer` thread runs; no crashes

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
