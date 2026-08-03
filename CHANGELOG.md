# Changelog

All notable changes to **P-Wall** are documented here.

## [1.0.0] - Prompt 4 (Manual Depth Editor)

### Added
- Manual Depth Editor (`ui/customize/MaskEditorScreen.kt` + `MaskEditorViewModel`)
  - Full-screen preview editor: shows the wallpaper with the extracted subject
    composited using the same `backgroundMatrix` as the live wallpaper, so the
    mask stays aligned with the image
  - Expand / Shrink sliders (grow or erode the subject mask)
  - Feather slider (box-blur the alpha for soft edges) + Smooth slider
    (blur + threshold for cleaner binary edges)
  - Reset (restores the unedited AI mask) + Save (persists the edit and reloads
    the wallpaper immediately) + Cancel
  - "Edit foreground mask" button in the AI Depth section of Customize
- Pure, JVM-testable mask math (`service/depth/MaskOps.kt`): sliding-window
  dilation/erosion + separable box blur / smooth over an alpha buffer —
  primitives future tools (brush, eraser, polygon selection) can build on
- `service/depth/MaskMath.kt`: bitmap <-> alpha-channel conversions +
  `eraseSubject` (moved from `MlKitSubjectSegmenter`); editing rewrites only the
  foreground's alpha channel so the subject keeps its real pixels
- Edited-mask storage in `DiskMaskStore` (`edited_foreground.png` /
  `edited_background.png` per image key); `load` transparently prefers an edited
  mask over the AI original, `deleteEdited` on Reset+Save
- `DepthEngine.reloadFromCache()` + a shared `maskEditNotifier` in `AppContainer`
  so the live wallpaper service picks up saved/reset edits instantly (no
  re-segmentation)
- Unit tests: `MaskOpsTest` (expand/shrink/feather/smooth, radius bounds,
  no-op radii, binary output)

### Verified
- `assembleDebug` + `assembleRelease` + `testDebugUnitTest` + `lintDebug`: 0 errors
- Unit tests: 79/79 pass (71 previous + 8 new)
- Lint: 0 errors (15 warnings / 4 hints — same as baseline, none in new code)
- On-device verification pending (no device attached during this pass)

## [1.0.0] - Prompt 3 (AI Depth Engine)

### Added
- AI Depth: on-device subject extraction that hides the clock *behind* people,
  pets and objects
  - `service/depth/` package:
    - `DepthSegmenter` interface (pluggable backend, automatic fallback on any
      failure) + `MlKitSubjectSegmenter` (Google ML Kit subject segmentation,
      `enableForegroundBitmap()`; install-time model download via manifest
      meta-data; background extraction punches a subject-shaped hole with
      `PorterDuff.Mode.DST_OUT`)
    - `MaskKeys`: stable disk-cache keys derived from image path + last-modified
    - `DiskMaskStore`: PNG cache in `files/depth_masks/<key>/` (foreground +
      background), reused across sessions
    - `DepthEngine`: orchestrator — segments **only when the wallpaper image
      changes** (never per frame), serves cached results, clears instantly when
      disabled
  - Render integration: `RenderFrame.foregroundBitmap` + a single shared
    `backgroundMatrix` (background transform + parallax shift) used by both
    `BackgroundLayer` and `ForegroundLayer`, so the extracted subject stays
    pixel-aligned with the image even while tilted; `ForegroundLayer` composites
    the subject above clock/date (clock behind subject). No-op when depth is off
    or unsupported → identical to plain rendering
  - `PWallWallpaperService`: depth engine wired into the image-change path only;
    toggling the feature re-runs segmentation for the current image, disabling
    drops the subject immediately
- AI Depth section in Customize: enable switch + explanatory text
- Settings: `depthEnabled` (default off) persisted via DataStore
- Unit tests: `MaskKeysTest` (stable, content-addressing) +
  `backgroundPanBounds` coverage in `WallpaperRendererTest`

### Verified
- `assembleDebug` + `testDebugUnitTest` + `lintDebug`: 0 errors
- Unit tests: 71/71 pass (62 previous + 9 new)
- Lint: 0 errors (15 warnings / 4 hints — same as baseline, none in new code)
- On-device functional pass (API 33):
  - AI Depth section renders; switch persists (`depth_enabled` in DataStore)
  - ML Kit subject segmentation runs exactly once per image change (single
    `subject_segmentation` module load in logcat, never per frame)
  - Masks cached to disk and reused; alpha-channel analysis confirmed the
    foreground is the subject-only bitmap and the background has a matching
    subject-shaped transparent hole
  - `pwall-renderer` thread stays alive with depth on; no crashes/ANRs
  - Settings reverted to default (depth off) after testing

## [1.0.0] - Prompt 2 (Premium 3D Engine)

### Added
- 3D parallax: subtle motion-driven background + clock movement
  - `service/motion/` package:
    - `ParallaxMath`: pure math (EMA smoothing filter, tilt→amplitude mapping,
      pan-room clamping) — JVM unit-tested, no Android types
    - `MotionFrame`: normalized tilt snapshot (+ `NONE`)
    - `MotionSource` interface, `SensorMotionProvider` (accelerometer primary +
      gyroscope secondary, delivered on a dedicated `pwall-motion` thread,
      `SENSOR_DELAY_GAME`, no wakelocks), `ParallaxController` (lifecycle,
      active/moving/idle states, settings push)
  - Engine integration: `WallpaperRenderEngine(motionSource)` injects motion
    into the frame; `BackgroundLayer` shifts the image within its real pan
    overflow (per background mode, clamped to room left after user pan);
    `ClockBlockLayout` shifts the clock block in the opposite direction
    (35% depth factor, re-clamped so it never leaves the screen)
  - Battery-aware frame pacing in `PWallWallpaperService`: ~30 fps while
    moving, ~5 fps while settling, 1 fps when idle; sensors registered only
    while the wallpaper is visible; auto-disabled on devices without motion
    sensors
- 3D Parallax section in Customize: enable switch + Sensitivity / Strength /
  Motion smoothing sliders (+ "not supported" notice on sensorless devices)
- Settings: `parallaxEnabled` (default off), `parallaxSensitivity`,
  `parallaxStrength`, `parallaxSmoothing` (default 0.5) persisted via DataStore
- Unit tests: `ParallaxMathTest` (smoothing, mapping, clamps, pan room)

### Verified
- `assembleDebug` + `testDebugUnitTest` + `lintDebug`: 0 errors
- Unit tests: 62/62 pass (45 baseline + 17 new)
- Lint: 0 errors (15 warnings / 4 hints — same as baseline)
- On-device functional pass (API 33):
  - 3D Parallax section renders; switch + sliders work; values persist
    (DataStore shows all four parallax keys)
  - With parallax enabled and wallpaper visible: `pwall-motion` + `pwall-renderer`
    threads run; accelerometer listener registered at `samplingPeriod=20000us`
    with `WakeLockRefCount 0`
  - Off-screen (app in foreground): sensor listener unregistered, render
    thread stopped — battery-safe
  - Settings reverted to default (parallax off) after testing; no crashes

## [1.0.0] - Sprint 7 (Modular Render Engine)

### Added
- Modular wallpaper render engine (`service/render/`)
  - `WallpaperRenderEngine`: composes layers + effects into a single frame
  - `LayerSystem`: ordered z-stack (later layers draw on top)
  - `EffectManager`: post-layer effects (no-op by default — seam for premium
    features like particles / vignette / weather)
  - `ModuleSystem`: self-contained feature bundles; duplicate ids rejected
  - `RenderFrame`: immutable per-frame context (settings, bitmap, time,
    density, dimensions); clock block resolved lazily once per frame
  - `Layer` / `Effect` / `Module` interfaces
- Five default layers registered by `WallpaperCoreModule` (z-order):
  `BackgroundLayer`, `ClockLayer`, `DateLayer`, `ForegroundLayer` (no-op),
  `OverlayLayer` (no-op)
- `ClockBlockLayout`: shared clock + date layout math (block positioned as a
  single unit, time and date draw at shared baselines)
- Unit tests: `LayerSystemTest`, `EffectManagerTest`, `ModuleSystemTest`,
  `WallpaperRenderEngineTest`

### Changed
- `WallpaperRenderer` keeps its full math API (used by preview + tests) but
  now delegates drawing to the engine's layers
- `PWallWallpaperService` renders via `engine.render(canvas, frame)`

### Verified
- `clean assembleDebug` + `testDebugUnitTest` + `lintDebug`: 0 errors
- Unit tests: 45/45 pass
- On-device functional pass (API 33):
  - App launches; Home live-preview clock/date renders
  - Customize screen renders all sections; setting changes apply live
  - Settings persist across process restart (DataStore)
  - Apply Live Wallpaper → service active (`mWallpaperComponent` set)
  - `pwall-renderer` thread runs while the wallpaper is visible
  - No crashes / ANRs
- No UI or behavior changes (refactor only)

## [1.0.0] - Sprint 6 (Custom Background Mode)

### Added
- Custom background mode: manual zoom (1–8×), rotation (±45°), and pan in a
  full-screen editor
  - `BackgroundMode.CUSTOM` + persisted transform settings (zoom, rotation,
    translate fractions)
  - `FullScreenBackgroundEditor`: pinch/twist/drag gestures + Zoom/Rotate
    sliders + Cancel/Done
  - `WallpaperRenderer.customPanBounds` / `customBackgroundMatrix` shared by
    preview and live wallpaper
- Unit tests for custom pan bounds

### Verified
- 30/30 tests, 0 lint errors, on-device gesture + persistence verified

## [1.0.0] - Sprint 5 (Full-Screen Position Editor)

### Added
- Clock position editing moved to a full-screen editor with drag + tap
  positioning and Cancel/Done confirmation
- "Edit on full screen" entry from the Customize screen

### Verified
- 26/26 tests, on-device verified

## [1.0.0] - Sprint 4 (Clock Position Fixes)

### Changed
- Drag-to-position clamped so the clock never leaves the screen
  (`clampBlockTopLeft` shared by preview + engine)
- Tap-to-position support in the preview

### Verified
- 26/26 tests, on-device verified

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
