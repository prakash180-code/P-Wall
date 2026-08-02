# P-Wall — Roadmap

Development is done sprint-by-sprint. **Sprint 1 and Sprint 2 are complete.**

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

## Sprint 3 — Production Ready
- [ ] Live preview polish
- [ ] Dark mode polish
- [ ] Error handling & edge cases
- [ ] Performance tuning & code cleanup
- [ ] Release APK build
- [ ] Final documentation

## Future Ideas (architecture reserved, not implemented now)
- Supabase licensing / activation
- Premium features
- Weather / battery / calendar overlays
- Multiple clocks, analog / neon / flip clock
- Animated / GIF / video wallpapers
- Theme packs
- Cloud backup

## Notes / Open Items
- Customize preview drag competes with the scrollable list; consider
  gesture-claiming or a dedicated drag affordance in a later polish pass.
- `mipmap-anydpi-v26` triggers an `ObsoleteSdkInt` lint hint (kept for AAPT compatibility with this AGP version).
- Dependency version bump suggestions from lint are intentionally deferred (pinned to a tested stable set).
