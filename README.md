# P-Wall

**P-Wall** is a modern Android Live Wallpaper application that turns any image from your device into a beautiful live wallpaper with a real-time digital clock, date, and day overlay — fully customizable.

- **Package:** `com.prakash.pwall`
- **Min SDK:** 29 (Android 10)
- **Target SDK:** 36 (Android 16)
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 (no XML layouts)
- **Architecture:** Clean Architecture + MVVM + StateFlow + Repository Pattern

## Features

- Select any image (JPG / PNG / WEBP) from the gallery
  - Android Photo Picker with Storage Access Framework fallback
  - Safe storage in app-private storage (no permissions required)
- Real-time digital clock: `HH:MM:SS`, 12/24 hour, seconds on/off
- Date & day display in multiple formats
- Live preview that updates instantly on every setting change
- Full customization:
  - Font family, size, bold, italic
  - Clock and date color
  - Shadow (enable/disable, blur, offset, color)
  - Transparency
  - Position presets + drag & drop custom position
  - Background mode: Fit / Fill / Stretch / Center Crop / Custom
  - 3D parallax: motion-driven background + clock movement with sensitivity,
    strength, and smoothing controls (sensors auto-disabled on unsupported
    devices; battery-aware adaptive frame rate)
  - AI Depth: on-device subject extraction (Google ML Kit) that hides the clock
    behind people, pets and objects — segments only when the wallpaper changes,
    caches masks on disk, and falls back to plain rendering automatically
  - Manual Depth Editor: fix the AI result by expanding, shrinking, feathering or
    smoothing the foreground mask on a full-screen preview, then save (the live
    wallpaper updates immediately)
- Premium UI & effects:
  - Glass Clock: frosted-glass panel behind the clock with real backdrop blur,
    adjustable opacity / corner radius / border / glow
  - Dynamic Colors: clock and date automatically pick readable colors from the
    wallpaper's dominant palette (with a manual-color fallback)
  - Micro Animations: smooth second sweep, gentle digit cross-fade, and a subtle
    breathing pulse (battery-aware frame pacing)
  - Cinematic Zoom: slow Ken Burns sweep on the wallpaper (zoom-in / zoom-out /
    alternate)
- Apply as live wallpaper via the Android Live Wallpaper picker
- Dark / Light Material 3 theme (follows the system)
- All settings persisted with DataStore Preferences (restored automatically)
- R8-optimized release build (~2 MB) with resource shrinking

## Project Structure

```
app/src/main/java/com/prakash/pwall/
├── PWallApplication.kt        # Application + DI bootstrap
├── MainActivity.kt            # Single activity, Compose entry point
├── di/                        # Manual dependency container (AppContainer)
├── navigation/                # NavHost + destinations
├── license/                   # LicenseManager abstraction (offline by default)
├── data/
│   ├── model/                 # WallpaperSettings + enums
│   ├── repository/            # SettingsRepository (DataStore)
│   └── storage/               # ImageStore (image persistence + validation)
├── service/
│   ├── PWallWallpaperService.kt  # Live Wallpaper Service (adaptive render loop)
│   ├── WallpaperRenderer.kt      # Shared math + legacy drawing facade
│   ├── render/                   # Modular render engine
│   │   ├── WallpaperRenderEngine.kt  # Compose layers + effects per frame
│   │   ├── LayerSystem / EffectManager / ModuleSystem
│   │   ├── Layer / Effect / Module    # Modular seams for future features
│   │   ├── WallpaperCoreModule        # Default 5-layer stack
│   │   ├── PremiumEffectsModule       # Glass panel below the clock
│   │   ├── ClockBlockLayout           # Shared clock + date layout math
│   │   ├── RenderAnimation            # TimeTransition / Breathing / easing
│   │   ├── ClockDraw                  # Shared glow + alpha text drawing
│   │   └── layers/                    # Background / Glass / Clock / Date / Foreground / Overlay
│   ├── color/                     # Dynamic colors from the wallpaper
│   │   ├── DominantColorExtractor # Pure quantization of dominant colors
│   │   └── ColorPalette           # Palette + readable auto clock/date colors
│   ├── effects/                   # CinematicZoom (Ken Burns sweep)
│   └── motion/                   # 3D parallax engine
│       ├── ParallaxController    # Lifecycle + settings + active/moving/idle
│       ├── SensorMotionProvider  # Accelerometer + gyroscope listener thread
│       ├── ParallaxMath          # Pure math (smoothing, tilt mapping, clamping)
│       └── MotionFrame / MotionSource
│   └── depth/                    # AI Depth engine + mask editing
│       ├── DepthEngine           # Orchestrator (segments only on image change)
│       ├── DepthSegmenter        # Pluggable segmentation backend
│       ├── MlKitSubjectSegmenter # Google ML Kit subject segmentation
│       ├── MaskKeys              # Stable disk-cache keys for masks
│       ├── DiskMaskStore         # PNG mask cache (original + edited variants)
│       ├── MaskOps               # Pure alpha ops (expand/shrink/feather/smooth)
│       └── MaskMath              # Bitmap <-> alpha conversions + compositing
├── ui/
│   ├── home/                  # Home screen + ViewModel
│   ├── preview/               # Preview screen + ViewModel
│   ├── customize/             # Customization screen + Mask editor + ViewModels
│   └── components/            # Shared Compose components (incl. ColorPicker)
├── utils/                     # ImageLoader, BitmapCache, formatters
└── theme/                     # Material 3 theme
```

## Modular Render Engine

Rendering is split into a small engine that composes **layers** (drawable
units), **effects** (post-layer processing) and **modules** (feature bundles):

- `WallpaperRenderEngine.render(canvas, frame)` draws each registered layer in
  z-order and applies effects after each one.
- The core module ships the default stack: background → clock → date →
  foreground → overlay.
- `WallpaperRenderer` keeps the shared transform/positioning math used by both
  the Compose preview and the engine, so preview and live wallpaper always match.
- `RenderFrame` exposes a single shared `backgroundMatrix` (transform + parallax
  shift) so the AI-depth foreground subject stays pixel-aligned with the image.
- Future premium features (weather/battery overlays, particle effects, manual
  depth editor) plug in as new modules/layers/effects without touching the core.

## AI Depth Engine

The `service/depth/` package extracts the foreground subject of the wallpaper
image with Google ML Kit subject segmentation (people, pets, objects):

- Runs **once per wallpaper image change** — never per frame.
- Foreground (subject) and background (subject erased) masks are cached as PNGs
  in app-private storage keyed by image identity, so segmentation is not repeated.
- `ForegroundLayer` composites the subject above the clock and date, hiding them
  behind the subject.
- Automatic fallback: if the feature is off, the model is unavailable, or
  segmentation fails, the wallpaper renders exactly as before.
- The on-device model downloads at install time via a manifest meta-data entry.

## Manual Depth Editor

Open "Edit foreground mask" from the AI Depth section to fix the AI extraction:

- Full-screen preview composites the subject over the wallpaper with the same
  matrix the live wallpaper uses, so edits preview exactly as they will render.
- Expand / Shrink grow or erode the mask; Feather softens the edges; Smooth
  cleans jagged borders (all operate on the mask's alpha channel via pure,
  JVM-tested `MaskOps` math).
- Reset restores the original AI mask; Save persists the edit
  (`edited_foreground.png` / `edited_background.png`) and the wallpaper reloads
  it immediately.
- The edited mask is a stackable, alpha-buffer based model that future tools
  (brush, eraser, polygon selection) plug into without changing save/reset.

## Premium UI & Effects

The premium features plug into the render engine as extra layers/math without
touching the core stack:

- **Glass Clock** (`PremiumEffectsModule` + `GlassPanelLayer`): a frosted panel
  is inserted *below* the clock layer. The blur is a real backdrop blur — the
  wallpaper region behind the block is sampled into a small cached bitmap and
  upscaled, so it stays cheap even at full opacity. When the feature is off the
  layer draws nothing and the output is identical to the core render.
- **Dynamic Colors** (`service/color/`): the wallpaper image is quantized once
  per image change into a small palette (pure JVM math, unit-tested).
  `PremiumColors` then picks a readable clock/date color per frame —
  wallpaper-derived when the toggle is on, the manual color otherwise. Preview
  and live wallpaper share the same resolver.
- **Micro Animations** (`RenderAnimation` + `FrameTicker`): the service remembers
  the previous time/date text and cross-fades over 420 ms whenever it changes;
  a 4-second sine pulse adds a barely-there breathing scale/alpha. Frame pacing
  jumps to ~30 fps only while a fade runs and ~8 fps while breathing/zooming.
- **Cinematic Zoom** (`CinematicZoom`): pure Ken Burns math (zoom-in / zoom-out /
  alternate sine loop) applied as a post-scale on the shared `backgroundMatrix`,
  so the AI-depth foreground stays pixel-aligned while the camera sweeps.

## Tech Stack

| Concern        | Choice                                              |
|----------------|-----------------------------------------------------|
| UI             | Jetpack Compose, Material 3                          |
| Architecture   | MVVM, Clean Architecture, State Hoisting             |
| Async          | Coroutines + Flow / StateFlow                        |
| Persistence    | DataStore Preferences                                |
| Navigation     | Navigation Compose                                   |
| Build          | Gradle Kotlin DSL, Version Catalog (`libs.versions.toml`) |
| Licensing      | `LicenseManager` interface, `OfflineLicenseManager` (always activated) |

## Licensing

The app is fully offline and treats every user as authorized. All licensing
logic is behind the `LicenseManager` interface:

```kotlin
interface LicenseManager {
    fun isActivated(): Boolean
}
```

`OfflineLicenseManager` always returns `true`. A future `OnlineLicenseManager`
(e.g. Supabase) can replace it without touching the rest of the app.

## Build

```bash
# Build debug APK
gradlew.bat :app:assembleDebug

# Run unit tests
gradlew.bat :app:testDebugUnitTest

# Lint
gradlew.bat :app:lintDebug

# Release APK
gradlew.bat :app:assembleRelease
# output: app/build/outputs/apk/release/app-release.apk (~2 MB, R8 + shrinkResources)
```

> **Note:** the release build currently signs with the debug key as a
> placeholder. Configure a real signing config before publishing.

## Roadmap

See [TODO.md](TODO.md) for the sprint-by-sprint roadmap and
[CHANGELOG.md](CHANGELOG.md) for progress history.
