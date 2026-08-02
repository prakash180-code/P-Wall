# P-Wall — Roadmap

Development is done sprint-by-sprint. **Sprints 1–3 are complete.**

## Sprint 1 — Project Foundation ✅ DONE
- [x] Gradle project + wrapper + version catalog
- [x] Material 3 theme (dark/light)
- [x] Clean Architecture structure (di, data, ui, navigation, utils, theme)
- [x] MVVM + StateFlow + Coroutines
- [x] DataStore Preferences persistence
- [x] Gallery image picker (Photo Picker + SAF fallback)
- [x] Home screen
- [x] Preview screen (image + live clock + date)
- [x] LicenseManager abstraction + OfflineLicenseManager
- [x] Builds, tests, lint, emulator smoke test pass

## Sprint 2 — Wallpaper Engine ✅ DONE
- [x] Android Live Wallpaper Service (`PWallWallpaperService` + `WallpaperRenderer`)
- [x] Render selected image with background scaling (Fit/Fill/Stretch/Center Crop)
- [x] Digital clock + date rendered on the wallpaper (updates every second)
- [x] Customization screen:
  - [x] Font family / size / bold / italic
  - [x] Clock color / date color
  - [x] Shadow (blur, offset, color)
  - [x] Transparency
  - [x] Position presets + drag & drop
  - [x] Background mode
- [x] Performance: minimal redraws, battery-aware, pause when screen off
- [x] Orientation & lifecycle handling
- [x] Apply Wallpaper flow (launch live wallpaper picker)
- [x] Restore settings in wallpaper service (DataStore)

## Sprint 3 — Production Ready ✅ DONE
- [x] Live preview polish (home card shows the clock overlay over the image)
- [x] Dark mode polish (verified system night-mode toggle)
- [x] Error handling & edge cases
  - [x] Image validation on import (rejects non-image / corrupted files)
  - [x] Safe wallpaper-picker launch
  - [x] Wallpaper render loop survives transient canvas errors
  - [x] Image decode moved off the main thread
- [x] Performance tuning & code cleanup (dead code removed)
- [x] Release APK build (R8 + shrinkResources, 2.1 MB)
- [x] Final documentation

## Future Ideas (architecture reserved, not implemented now)
- Production signing config (keystore) before publishing
- Supabase licensing / activation
- Premium features
- Weather / battery / calendar overlays
- Multiple clocks, analog / neon / flip clock
- Animated / GIF / video wallpapers
- Theme packs
- Cloud backup

## Notes / Open Items
- Release build uses the debug signing key as a placeholder — configure a real
  keystore before publishing to any store.
- Customize preview drag competes with the scrollable list; consider
  gesture-claiming or a dedicated drag affordance in a later polish pass.
- `mipmap-anydpi-v26` triggers an `ObsoleteSdkInt` lint hint (kept for AAPT compatibility with this AGP version).
- Dependency version bump suggestions from lint are intentionally deferred (pinned to a tested stable set).
