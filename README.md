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
  - Background mode: Fit / Fill / Stretch / Center Crop
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
│   ├── PWallWallpaperService.kt  # Live Wallpaper Service (1s render loop)
│   ├── WallpaperRenderer.kt      # Shared math + legacy drawing facade
│   └── render/                   # Modular render engine
│       ├── WallpaperRenderEngine.kt  # Compose layers + effects per frame
│       ├── LayerSystem / EffectManager / ModuleSystem
│       ├── Layer / Effect / Module    # Modular seams for future features
│       ├── WallpaperCoreModule        # Default 5-layer stack
│       ├── ClockBlockLayout           # Shared clock + date layout math
│       └── layers/                    # Background / Clock / Date / Foreground / Overlay
├── ui/
│   ├── home/                  # Home screen + ViewModel
│   ├── preview/               # Preview screen + ViewModel
│   ├── customize/             # Customization screen + ViewModel
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
- Future premium features (3D parallax, depth engine, weather/battery overlays)
  plug in as new modules/layers/effects without touching the core.

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
