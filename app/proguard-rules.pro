# Add project specific ProGuard rules here.
# Keep line numbers for crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# The live wallpaper engine is referenced from the manifest, but keep it
# explicitly so R8 never drops the binder glue classes.
-keep class com.prakash.pwall.service.PWallWallpaperService { *; }
-keep class com.prakash.pwall.service.PWallWallpaperService$* { *; }
