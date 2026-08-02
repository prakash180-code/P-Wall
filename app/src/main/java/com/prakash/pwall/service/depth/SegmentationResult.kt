package com.prakash.pwall.service.depth

import android.graphics.Bitmap

/**
 * Result of a single foreground/background extraction pass. Both bitmaps are the
 * same size as the source image.
 *
 * - [foreground]: the subject only, everything else transparent (used to draw the
 *   clock *behind* the subject).
 * - [background]: the scene with the subject erased (a transparent hole where the
 *   subject was), cached for future effects / the manual depth editor.
 */
data class SegmentationResult(
    val foreground: Bitmap,
    val background: Bitmap
)
