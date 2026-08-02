# P-Wall — Roadmap

Development is done sprint-by-sprint. **Sprint 1 is complete.**

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

## Sprint 2 — Wallpaper Engine
- [ ] Android Live Wallpaper Service (`WallpaperService`)
- [ ] Render selected image with background scaling (Fit/Fill/Stretch/Center Crop)
- [ ] Digital clock + date rendered on the wallpaper (updates every second)
- [ ] Customization screen:
  - [ ] Font family / size / bold / italic
  - [ ] Clock color / date color
  - [ ] Shadow (blur, offset, color)
  - [ ] Transparency
  - [ ] Position presets + drag & drop
  - [ ] Background mode
- [ ] Performance: minimal redraws, battery-aware, pause when screen off
- [ ] Orientation & lifecycle handling

## Sprint 3 — Production Ready
- [ ] Live preview polish
- [ ] Apply Wallpaper flow (launch live wallpaper picker)
- [ ] Restore settings in wallpaper service
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
- Live wallpaper service and customization UI are the next (Sprint 2) scope.
- `mipmap-anydpi-v26` triggers an `ObsoleteSdkInt` lint hint (kept for AAPT compatibility with this AGP version).
- Dependency version bump suggestions from lint are intentionally deferred (pinned to a tested stable set).
