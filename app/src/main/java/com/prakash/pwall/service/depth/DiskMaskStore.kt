package com.prakash.pwall.service.depth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Persistent cache for extracted masks, stored as PNGs in an app-private directory
 * (files/depth_masks/<key>/). Foreground and background are separate files so the
 * manual depth editor (Prompt 4) can rewrite just the foreground mask.
 *
 * Each image key can hold two variants:
 * - the **original** AI mask (`foreground.png` / `background.png`), written by
 *   the depth pipeline and the target of "Reset";
 * - the **edited** mask (`edited_foreground.png` / `edited_background.png`),
 *   written by the manual depth editor.
 *
 * [load] returns the edited variant when present, otherwise the original, so the
 * renderer transparently prefers the user's manual corrections.
 */
class DiskMaskStore(context: Context) {

    private val root: File = File(context.filesDir, "depth_masks").apply { mkdirs() }

    /** Best mask available for [key]: edited first, then the AI original. */
    fun load(key: String): SegmentationResult? = loadEdited(key) ?: loadOriginal(key)

    /** The unedited AI mask (source of truth for "Reset"). */
    fun loadOriginal(key: String): SegmentationResult? =
        decodePair(File(root, key), FOREGROUND_FILE, BACKGROUND_FILE)

    /** The user-edited mask, if any. */
    fun loadEdited(key: String): SegmentationResult? =
        decodePair(File(root, key), EDITED_FOREGROUND_FILE, EDITED_BACKGROUND_FILE)

    fun save(key: String, result: SegmentationResult) {
        val dir = File(root, key).apply { mkdirs() }
        compress(dir, FOREGROUND_FILE, result.foreground)
        compress(dir, BACKGROUND_FILE, result.background)
    }

    fun saveEdited(key: String, result: SegmentationResult) {
        val dir = File(root, key).apply { mkdirs() }
        compress(dir, EDITED_FOREGROUND_FILE, result.foreground)
        compress(dir, EDITED_BACKGROUND_FILE, result.background)
    }

    fun deleteEdited(key: String) {
        val dir = File(root, key)
        File(dir, EDITED_FOREGROUND_FILE).delete()
        File(dir, EDITED_BACKGROUND_FILE).delete()
    }

    fun hasEdited(key: String): Boolean {
        val dir = File(root, key)
        return File(dir, EDITED_FOREGROUND_FILE).exists() ||
            File(dir, EDITED_BACKGROUND_FILE).exists()
    }

    fun delete(key: String) {
        File(root, key).deleteRecursively()
    }

    private fun decodePair(dir: File, fgName: String, bgName: String): SegmentationResult? {
        if (!dir.isDirectory) return null
        val fg = decode(File(dir, fgName)) ?: return null
        val bg = decode(File(dir, bgName)) ?: return null
        return SegmentationResult(fg, bg)
    }

    private fun compress(dir: File, name: String, bitmap: Bitmap) {
        runCatching {
            File(dir, name).outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private fun decode(file: File): Bitmap? {
        if (!file.exists()) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }

    private companion object {
        const val FOREGROUND_FILE = "foreground.png"
        const val BACKGROUND_FILE = "background.png"
        val EDITED_FOREGROUND_FILE = "${MaskKeys.EDITED}_foreground.png"
        val EDITED_BACKGROUND_FILE = "${MaskKeys.EDITED}_background.png"
    }
}
