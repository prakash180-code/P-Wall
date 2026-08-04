# ============================================================================
# P-Wall ProGuard / R8 rules
#
# Release builds run R8 with:
#   - minification  (obfuscation + dead-code elimination)
#   - optimization
#   - resource shrinking
#
# Android components referenced from AndroidManifest.xml are kept automatically
# by R8 (application, activity, service). The rules below protect the pieces R8
# cannot see from static analysis. Every custom keep rule is documented.
# ============================================================================

# ---- Crash reporting readability ------------------------------------------
# Keep the original source file name and line numbers so stack traces in
# release builds still map back to source.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Live wallpaper binder glue --------------------------------------------
# PWallWallpaperService is instantiated by the Android system through the
# WallpaperService binder, not by app code, and its nested classes (Engine,
# etc.) are created reflectively by the framework. Keep them fully so R8 never
# renames or strips the classes the binder needs.
-keep class com.prakash.pwall.service.PWallWallpaperService { *; }
-keep class com.prakash.pwall.service.PWallWallpaperService$* { *; }

# The wallpaper render engine is reached from the service and wires layers by
# id. It is kept by reachability, but keep it intact defensively.
-keep class com.prakash.pwall.service.render.WallpaperRenderEngine { *; }

# ---- DataStore --------------------------------------------------------------
# androidx.datastore.preferences ships consumer ProGuard rules, so no broad
# keep is required. We still pin the public Preferences API class so the
# DataStore codec keeps working after R8 optimizations (no reflection is used,
# but this documents the intent and guards the persisted format).
-keep class androidx.datastore.preferences.core.** { *; }

# ---- Google ML Kit subject segmentation -------------------------------------
# The client is a thin binding over Play services, and the internal model
# provider classes are looked up reflectively at runtime. Keep the whole
# segmentation package (public client + internal model binding) so AI depth
# keeps working after obfuscation. The (large, downloaded) model is not part of
# the APK.
-keep class com.google.mlkit.vision.segmentation.** { *; }
-dontwarn com.google.mlkit.**

# ---- Enums used by name in the JSON settings backup -------------------------
# SettingsBackup encodes every app enum by its name (Enum.name) for the
# backup / restore feature, and the app enums are referenced by name nowhere
# else at runtime. R8's enum optimization could rewrite them; pin the enum
# machinery so Enum.name round-trips survive. (The values()/valueOf() member
# rule is already in AGP's default optimize config; this protects the enums
# themselves.)
-keepclassmembers enum com.prakash.pwall.data.model.** { *; }

# ---- App settings model -----------------------------------------------------
# WallpaperSettings is a pure data snapshot (immutable constructor args). Keep
# its members so the DataStore codec and the settings screens keep working
# after obfuscation.
-keepclassmembers class com.prakash.pwall.data.model.WallpaperSettings { *; }

# ---- Compose ----------------------------------------------------------------
# Jetpack Compose ships its own consumer ProGuard rules that AGP merges
# automatically. Keep the runtime tokens that are looked up by the animation
# machinery at runtime.
-keepclassmembers class androidx.compose.runtime.** { *** Companion; }

# ---- BuildConfig ------------------------------------------------------------
# BuildConfig.VERSION_NAME / BuildConfig.DEBUG are read at runtime.
-keep class com.prakash.pwall.BuildConfig { *; }
