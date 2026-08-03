# P-Wall — Roadmap

Development is done prompt-by-prompt. **Sprints 1–7 and Prompts 2–4 are complete.**

## Prompt 4 — Manual Depth Editor ✅ DONE
- [x] Preview editor: full-screen preview with the subject composited over the
      wallpaper (same `backgroundMatrix` as the live wallpaper)
- [x] Expand mask (sliding-window dilation, `MaskOps.expand`)
- [x] Shrink mask (sliding-window erosion, `MaskOps.shrink`)
- [x] Feather edges (box-blur the alpha, `MaskOps.feather`)
- [x] Smooth edges (blur + threshold to clean binary edges, `MaskOps.smooth`)
- [x] Reset mask (restores the unedited AI mask; saving a reset removes the edit)
- [x] Save edited mask (`edited_foreground.png` / `edited_background.png` in
      `DiskMaskStore`; `DepthEngine.reloadFromCache()` via `maskEditNotifier`
      updates the live wallpaper immediately)
- [x] "Edit foreground mask" entry in the AI Depth section of Customize
- [x] Architecture reserved for Brush / Eraser / Polygon selection (alpha-buffer
      editing model + pure `MaskOps` primitives) — not implemented
- [x] 79/79 tests, 0 lint errors, debug + release APKs build

## Prompt 3 — AI Depth Engine ✅ DONE
- [x] Google ML Kit subject segmentation (`service/depth/MlKitSubjectSegmenter`)
- [x] Offline, on-device processing (model via Play services, install-time download)
- [x] Foreground extraction (subject-only bitmap) + background extraction
      (subject-shaped transparent hole via `DST_OUT`)
- [x] Cached masks on disk keyed by image identity (`MaskKeys` + `DiskMaskStore`)
- [x] Clock hidden behind the foreground object (ForegroundLayer above clock/date,
      shared `backgroundMatrix` keeps subject aligned incl. parallax)
- [x] Automatic fallback to plain rendering when off / unsupported / fails
- [x] Automatic caching (segments once per image change, then reuses the cache)
- [x] Segmentation only when wallpaper changes — never per frame
- [x] "AI Depth" section in Customize + `depthEnabled` DataStore setting
- [x] 71/71 tests, 0 lint errors, on-device verified

## Prompt 2 — Premium 3D Engine ✅ DONE
- [x] Accelerometer + gyroscope driven movement (`service/motion/`)
- [x] `ParallaxMath` pure-math module (smoothing filter, tilt mapping, clamping)
- [x] Background movement within real pan overflow (all background modes)
- [x] Foreground (clock) movement in the opposite direction (35% depth factor)
- [x] Sensitivity / Strength / Motion smoothing settings + enable switch in
      Customize ("3D Parallax" section)
- [x] Battery-aware adaptive frame pacing (~30 fps moving / ~5 fps settling /
      1 fps idle)
- [x] Sensors only while visible; auto-disabled on unsupported devices
- [x] 62/62 tests, 0 lint errors, on-device verified

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

## Sprint 4 — Clock Position Fix ✅ DONE
- [x] Drag-to-position clamped so the clock stays on-screen (`clampBlockTopLeft`)
- [x] Tap-to-position in the preview
- [x] 26/26 tests, on-device verified

## Sprint 5 — Full-Screen Position Editor ✅ DONE
- [x] Clock position editing on a full-screen preview with drag + tap + Cancel/Done
- [x] 26/26 tests, on-device verified

## Sprint 6 — Custom Background Mode ✅ DONE
- [x] `BackgroundMode.CUSTOM` (zoom 1–8×, rotation ±45°, pan)
- [x] Full-screen editor with gestures + sliders
- [x] Shared transform math between preview and live wallpaper
- [x] 30/30 tests, on-device verified

## Sprint 7 — Modular Render Engine ✅ DONE
- [x] `WallpaperRenderEngine` (compose layers + effects + render)
- [x] Layer system (z-ordered), module system (duplicate rejection),
      effect manager (no-op by default)
- [x] Core layers: Background / Clock / Date / Foreground / Overlay
- [x] `WallpaperRenderer` math API preserved (preview + tests untouched)
- [x] No UI or behavior changes; 45/45 tests; on-device verified
- [x] Foundation for premium modules (next: 3D parallax, depth engine)

## Future Ideas (architecture reserved, not implemented now)
- Brush / Eraser / Polygon selection in the depth editor (alpha-buffer model ready)
- Production signing config (keystore) before publishing
- Supabase licensing / activation
- Premium features (weather / battery / calendar overlays) — new layers/effects
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
