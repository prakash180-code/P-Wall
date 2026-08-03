package com.prakash.pwall.service.depth

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * [DepthSegmenter] backed by ML Kit subject segmentation
 * (com.google.android.gms:play-services-mlkit-subject-segmentation).
 *
 * The model is unbundled: it is downloaded through Google Play services at install
 * time (via the manifest meta-data com.google.mlkit.vision.DEPENDENCIES=subject_segment)
 * or lazily on first use. Until it is available, [segment] returns null and the
 * renderer keeps showing the plain image (automatic fallback).
 */
class MlKitSubjectSegmenter : DepthSegmenter {

    private val segmenter: SubjectSegmenter? by lazy {
        runCatching {
            SubjectSegmentation.getClient(
                SubjectSegmenterOptions.Builder()
                    .enableForegroundBitmap()
                    .build()
            )
        }.getOrNull()
    }

    override fun isSupported(): Boolean = segmenter != null

    override suspend fun segment(bitmap: Bitmap): SegmentationResult? = withContext(Dispatchers.IO) {
        val client = segmenter ?: return@withContext null
        val foreground = runCatching {
            val image = InputImage.fromBitmap(bitmap, 0)
            Tasks.await(client.process(image), 30, TimeUnit.SECONDS)
        }.getOrNull()?.foregroundBitmap ?: return@withContext null

        val background = MaskMath.eraseSubject(bitmap, foreground)
        SegmentationResult(foreground = foreground, background = background)
    }

    override fun close() {
        runCatching { segmenter?.close() }
    }
}
