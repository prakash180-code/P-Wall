# Changelog

All notable changes to **P-Wall** are documented here.

## [1.0.1] - Stable Release Preparation

A stability, usability and release-preparation update. No features were
removed and no behavior was rewritten.

### Improvements
- **Version bump** to `1.0.1` (versionCode 2).
- **About screen** now shows the app version (read from the package, with a
  `BuildConfig` fallback), the app branding, and the developer (Prakash).
- **Icons**: the Settings card on Home uses a dedicated gear icon instead of
  reusing the tuning icon; hero buttons use a consistent corner radius.
- **Documented ProGuard rules** (`proguard-rules.pro`) with a comment on every
  keep rule, covering the live-wallpaper binder glue, the render engine,
  DataStore, ML Kit subject segmentation, the enum-name-based settings backup,
  the settings model, Compose runtime tokens and `BuildConfig`.

### Performance
- `ClockTextFormatter` now caches the `DateTimeFormatter` instances (immutable
  and thread-safe) instead of building them on every format call from the
  render thread and preview.
- Narrowed the release keep rules so R8 can still shrink unused code: the
  release APK dropped from ~4.45 MB to **3.37 MB** while preserving all
  required classes.

### Bug Fixes
- **Settings backup on device transfer/cloud restore**: the backup rules
  included a `sharedpref` domain that the app does not use (settings live in
  DataStore). `backup_rules.xml` / `data_extraction_rules.xml` now include the
  DataStore directory, the wallpaper image and the depth-mask cache, so a
  restored device keeps the user's settings and image.

### Security
- Release builds explicitly set `isDebuggable = false` / `isJniDebuggable =
  false`; verified the built APK carries no `application-debuggable="true"`
  flag.
- Audit confirms no hardcoded secrets, API keys, or passwords in source or
  resources (the app makes no network calls and uses an offline license).
- Diagnostic logs are centralised in `PWallLog`; verbose `d()` output is
  compiled out of release builds via `BuildConfig.DEBUG`.
- R8 keeps `PWallWallpaperService` + nested classes, `WallpaperRenderEngine`,
  DataStore, ML Kit segmentation and the enum backup machinery intact
  (verified in the release `mapping.txt`).

### Storage Improvements
- Full storage audit: every file P-Wall writes is already in app-private
  storage under `Android/data/com.prakash.pwall/files/` (nothing in shared
  storage):
  - `datastore/pwall_settings.preferences_pb` — settings (permanent)
  - `wallpaper_images/selected_image.*` — the picked wallpaper (permanent)
  - `depth_masks/<key>/` — AI masks + manual edits (derived cache)
  - `profileInstalled`, `profileinstaller_*.dat`,
    `phenotype_storage_info/` — library-managed internals (cache)
- The files listed in the audit prompt (`ui.xml`, `ui0.xml`, `d1.xml`,
  `rel1.xml`, `dark.xml`, `.segments`) are not created by P-Wall.

### Verified
- `assembleDebug` + `assembleRelease` + `testDebugUnitTest` + `lintDebug`:
  0 errors. Unit tests **156/156** pass; lint 0 errors / 15 warnings
  (baseline unchanged).
- Release APK (R8 + ProGuard + shrinkResources + resource optimization):
  3.37 MB, versionName 1.0.1, versionCode 2, not debuggable.
- On-device release validation (API 33 physical device):
  - APK installs and launches; all five screens navigate without crashes
  - Live wallpaper applied; `pwall-motion` + `pwall-renderer` threads active
    on the home screen; clock/date overlay live; no FATAL/ANR
  - AI Depth toggles on/off without crashing (masks cached on disk); parallax
    debug overlay + settings persist via DataStore
- Git tag `v1.0.1` pushed to `origin/main`.

## [2.0.0] - Prompt 7 (Full UI Redesign)

### Added
- Bottom navigation shell with five destinations (`PWallNavHost`):
  Home, Clock, Effects, Background, Settings — each with its own toolbar,
  hero preview, and a consistent Material 3 card grid
- New `AppSettingsViewModel` (shared across every screen) with
  `settings.asStateFlow()` + typed setters for all persisted preferences
  (deduplicated the old per-screen ViewModels)
- Shared UI components (`ui/components/SettingsUi.kt`):
  `SettingsCard`, `SettingsSection`, `SwitchRow`, `SliderRow`, `SegmentedRow`,
  `ColorField` (opens the color picker), `TextButtonRow`, `ChipGroup`,
  `InfoBanner` — used by all settings screens
- New `ui/editors/FullScreenEditors.kt` combining the full-screen background
  and position editors (one `FullScreenEditor` shell, real preview, Cancel/Done)
- Dynamic Material 3 theme (`theme/Theme.kt` + `MainActivity`)
  - `ThemeMode` AUTO / LIGHT / DARK / CUSTOM; custom accent picked from the
    wallpaper's dominant color (reuses `DominantColorExtractor`)
  - Dynamic color from the wallpaper when a custom theme accent is enabled
- Home redesign
  - Phone-frame hero preview (`PhoneFramePreview`) with a live engine-rendered
    preview, "Change image" + "Open full preview" buttons
  - Colorful 2×2 cards: Clock, Effects, Background, Settings (each tinted with
    a per-card gradient, swatches show the current accent)
  - "Apply Live Wallpaper" action on the hero
- Clock screen: layout (presets + full-screen editor), clock format
  (hours/seconds/24h, date formats), font family, size, bold/italic, clock +
  date colors (with dynamic-color preview), shadow (toggle, blur, offset, color,
  alpha), transparency slider
- Effects screen: 3D Parallax (enable, sensitivity, strength, smoothing, debug
  `ParallaxMath` amplification), AI Depth (enable + "Edit foreground mask"),
  Glass Clock (opacity, corner radius, border, glow), Dynamic Colors,
  Micro Animations, Cinematic Zoom (direction chips, speed, zoom), performance
  preset (Auto/On/Off)
- Background screen: background mode chips (Fit/Fill/Stretch/Center Crop/Custom)
  + full-screen custom editor; position preset; zoom/rotation/pan sliders
- Settings screen: theme mode (Auto/Light/Dark/Custom + custom accent),
  backup/restore (JSON snapshot of all preferences), storage usage + "Clear
  image" (deletes the imported wallpaper), about dialog, reset to defaults
- Screens live-update the moment a value changes (shared DataStore-backed flow);
  preview and wallpaper render engine untouched
- Unit tests: `debugShiftPx` / `debugForegroundShift` coverage in
  `ParallaxMathTest`

### Changed
- Navigation is now an activity-wide scaffold with bottom tabs instead of the
  push/pop Home → Customize flow
- `ui/customize/CustomizeScreen` + `CustomizeViewModel` reworked onto the shared
  `AppSettingsViewModel` / `SettingsUi` components (survives as the Clock screen
  tab entry point for legacy flows)
- Deleted `ui/home/HomeViewModel` settings push (folded into
  `AppSettingsViewModel`)

### Verified
- `assembleDebug` + `testDebugUnitTest` + `lintDebug`: 0 errors
- Unit tests: all pass (parallax debug-math cases added)
- On-device smoke test (physical device, API 33): every tab (Home / Clock /
  Effects / Background / Settings), the full-screen preview, and the Apply
  Live Wallpaper action all navigate without crashes; no FATAL in logcat

## [1.1.0] - Prompt 6 (Production Optimization)

### Added
- Paint cache (`utils/PaintCache.kt` + `service/render/PaintKey.kt`)
  - Per-engine, bounded, thread-safe paint reuse keyed by an exact
    configuration signature; `ClockBlockLayout` now reuses the clock/date/glow
    paints instead of allocating them every frame
  - The glass glow paints are resolved once per frame in the shared clock
    `Block` and reused by both the clock and date layers (2 fewer allocations
    per frame after the first)
- Performance logic (`service/performance/`)
  - `FramePacer`: pure, battery-aware frame pacing (parallax moving ~30 fps,
    settling ~5 fps, idle 1 fps; ~30 fps during cross-fades; ~8 fps breathing/
    zoom; always 1 fps in low-end mode)
  - `FrameDirtyChecker`: skips a frame when nothing that affects pixels changed;
    the key is only recorded after the frame is actually posted, so a failed
    surface lock never causes a missed redraw
  - `LowEndDevice`: resolves the low-end profile from the new `lowEnd` setting
    (Auto/On/Off) + the device memory class / low-RAM flag, and derives the
    "effective" settings (heavy per-frame effects disabled)
  - `RenderGuard` (`service/render/`): circuit breaker that drops a layer after
    repeated draw exceptions so one bad layer cannot crash the render thread
- Render engine hardening (`WallpaperRenderEngine`)
  - Per-engine `PaintCache` (preview and wallpaper never share paints across
    threads), per-layer circuit breaker with logging, and `release()` to free
    native resources held by `Releasable` layers
  - `GlassPanelLayer` now caches its panel/border paints and round-rect clip
    path, and implements `Releasable` (scratch bitmaps recycled on destroy)
- Memory optimization
  - `BitmapCache` sized from the ActivityManager memory class (~1/8, 16–48 MB
    bounds) with a `trim()` half-eviction and `configure()` re-size
  - `PWallApplication.onTrimMemory` trims/clears the bitmap cache at the
    appropriate memory levels
  - Low-end devices decode the wallpaper at a reduced cap
    (`LowEndDevice.LOW_END_MAX_DIMENSION` = 1280) via the new
    `ImageLoader.decodeSampled(path, maxDimension)`
- Render-loop optimization (`PWallWallpaperService`)
  - Hardware canvas (`lockHardwareCanvas`, API 30+) with software fallback
  - Render-loop backoff (2 s sleep after 5 consecutive failed frames)
  - Lifecycle logging via `PWallLog`
- Settings/UI: `LowEndPreference` model + DataStore persistence + `setLowEnd`
  ViewModel setter + a new "Performance" section in Customize (Auto/On/Off chips)
- Unit tests: `FramePacerTest`, `FrameDirtyCheckerTest`, `LowEndDeviceTest`,
  `RenderGuardTest`, `PaintKeyTest`

### Verified
- `assembleDebug` + `testDebugUnitTest` + `lintDebug`: 0 errors
- Unit tests: 143/143 pass (107 previous + 36 new)
- Lint: 0 errors, 15 warnings, 4 hints (baseline unchanged)
- Release APK: `app/build/outputs/apk/release/app-release.apk` (3.24 MB,
  R8 + shrinkResources)

## [1.0.0] - Prompt 5 (Premium UI & Effects)

### Added
- Glass Clock (`service/render/layers/GlassPanelLayer.kt`)
  - Frosted-glass panel behind the clock: real backdrop blur (samples the
    wallpaper region into a small cached bitmap, upscaled with filtering),
    adjustable panel opacity, corner radius, border color + width, and a soft
    glow behind the time
  - Registered below the clock layer by the new `PremiumEffectsModule` via
    `LayerSystem.insertBefore`, so the default render is unchanged whenever the
    feature is off
- Dynamic Colors (`service/color/`)
  - `DominantColorExtractor`: pure, JVM-testable dominant-color extraction
    (downsample → coarse RGB histogram → distance-based merging; fixed a
    bucket-average bug the tests caught)
  - `ColorPalette`: dominant / most-saturated / average luminance + readable
    auto clock and date colors derived from the wallpaper
  - `PremiumColors`: resolves the final per-frame text color — wallpaper-derived
    when dynamic is enabled, the user's manual color otherwise (shared by
    preview and engine so they always agree)
- Micro Animations (`service/render/RenderAnimation.kt` + `ClockDraw.kt`)
  - `TimeTransition`: 420 ms ease-out cubic cross-fade whenever the digits change
  - `Breathing`: 4 s sine pulse with a tiny scale + alpha wobble
  - `FrameTicker` in the service tracks the previous/current text; the render
    loop paces at ~30 fps during a fade and ~8 fps while breathing/zooming
  - `ClockLayer` / `DateLayer` share a two-pass glow + alpha draw
- Cinematic Zoom (`service/effects/CinematicZoom.kt`)
  - Pure Ken Burns math (zoom-in / zoom-out / alternate sine loop), applied as a
    post-scale on the shared `backgroundMatrix` so the depth foreground stays
    aligned while the camera sweeps
- Premium Settings
  - Four new Customize sections: Glass Clock, Dynamic Colors, Micro Animations,
    Cinematic Zoom — full sliders, switches, color pickers and direction chips
    backed by `CustomizeViewModel` setters + DataStore persistence
- Previews upgraded
  - `WallpaperPreview` now draws through the same `WallpaperRenderEngine` as the
    live wallpaper (glass, dynamic colors, animations, zoom all visible)
  - `ClockOverlay` gains smooth seconds, digit cross-fade, breathing and glass
    glow in Compose
- Unit tests: `CinematicZoomTest`, `AnimationMathTest`, `ColorPaletteTest`,
  `DominantColorExtractorTest`, plus `insertBefore` coverage in `LayerSystemTest`
  and `WallpaperRenderEngineTest`

### Verified
- `assembleDebug` + `testDebugUnitTest` + `lintDebug`: 0 errors
- Unit tests: 107/107 pass (79 previous + 28 new)
- Lint: 0 errors (dependency-version notices only, same as baseline)
- On-device verification (API 33 emulator):
  - All four premium sections render in Customize (Glass Clock, Dynamic Colors,
    Micro Animations, Cinematic Zoom) with their descriptions
  - Glass panel (opacity 35%, blur 14, corner 24, border 2.0), dynamic clock
    color, fade transitions, smooth seconds, breathing (0.35) and cinematic zoom
    (50%, 30s, alternate) all enabled via UI and confirmed persisted in the
    DataStore preferences file
  - Live wallpaper applied; `pwall-renderer` engine thread active; frames
    changing between captures (animations live); no FATAL/crashes in logcat

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
