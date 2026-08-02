package com.prakash.pwall.service.depth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Persistent cache for extracted masks, stored as PNGs in an app-private directory
 * (files/depth_masks/<key>/). Foreground and background are separate files so the
 * manual depth editor (Prompt 4) can rewrite just the foreground mask.
 */
class DiskMaskStore(context: Context) {

    private val root: File = File(context.filesDir, "depth_masks").apply { mkdirs() }

    fun load(key: String): SegmentationResult? {
        val dir = File(root, key)
        if (!dir.isDirectory) return null
        val fg = decode(File(dir, "foreground.png")) ?: return null
        val bg = decode(File(dir, "background.png")) ?: return null
        return SegmentationResult(fg, bg)
    }

    fun save(key: String, result: SegmentationResult) {
        val dir = File(root, key).apply { mkdirs() }
        runCatching { File(dir, "foreground.png").outputStream().use { result.foreground.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        runCatching { File(dir, "background.png").outputStream().use { result.background.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }

    fun delete(key: String) {
        File(root, key).deleteRecursively()
    }

    private fun decode(file: File): Bitmap? {
        if (!file.exists()) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }
}
