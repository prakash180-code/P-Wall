package com.prakash.pwall.ui.customize

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prakash.pwall.service.depth.DiskMaskStore
import com.prakash.pwall.service.depth.MaskMath
import com.prakash.pwall.service.depth.MaskOps
import com.prakash.pwall.service.depth.SegmentationResult
import com.prakash.pwall.utils.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Snapshot of everything the mask editor screen renders. */
data class MaskEditorUiState(
    val loading: Boolean = true,
    val ready: Boolean = false,
    val noMask: Boolean = false,
    val busy: Boolean = false,
    val dirty: Boolean = false,
    val workingForeground: Bitmap? = null,
    val message: String? = null
)

/**
 * State holder for the manual depth editor (Prompt 4).
 *
 * The edited mask is a copy of the AI foreground whose alpha channel is mutated
 * by the pure [MaskOps] operations; the working copy is previewed live and only
 * committed to disk on [save]. "Reset" restores the unedited AI mask, and saving
 * a reset state removes any previously stored edit so the pipeline falls back
 * to the original.
 *
 * Future tools (brush, eraser, polygon selection) plug into this same model: they
 * stamp regions onto the working foreground's alpha buffer via [MaskMath] and
 * [MaskOps] without changing the save/reset flow.
 */
class MaskEditorViewModel(
    private val store: DiskMaskStore,
    private val key: String?,
    private val sourcePath: String?,
    private val notifier: MutableSharedFlow<Unit>
) : ViewModel() {

    private val _state = MutableStateFlow(MaskEditorUiState())
    val state = _state.asStateFlow()

    private var sourceBitmap: Bitmap? = null
    private var originalForeground: Bitmap? = null
    private var originalBackground: Bitmap? = null
    private var workingForeground: Bitmap? = null
    private var workingBackground: Bitmap? = null

    /**
     * (Re)loads the mask for the current image. Called once on first use and
     * again every time the editor is reopened, so a freshly saved or AI-generated
     * mask is always shown (the ViewModel itself is scoped to the screen and
     * reused across opens).
     */
    fun reload() {
        val k = key
        val path = sourcePath
        if (k == null || path == null) {
            _state.value = MaskEditorUiState(
                loading = false,
                noMask = true,
                message = "Select a wallpaper image before editing the mask."
            )
            return
        }
        _state.update { it.copy(loading = true, ready = false, noMask = false) }
        viewModelScope.launch {
            val original = withContext(Dispatchers.IO) { store.loadOriginal(k) }
            val current = withContext(Dispatchers.IO) { store.load(k) }
            val source = withContext(Dispatchers.IO) { ImageLoader.decodeSampled(path) }
            if (original == null || source == null) {
                _state.value = MaskEditorUiState(
                    loading = false,
                    noMask = true,
                    message = "No AI mask found yet. Enable AI Depth and change the " +
                        "wallpaper so it can extract the subject."
                )
                return@launch
            }
            sourceBitmap = source
            originalForeground = original.foreground
            originalBackground = original.background
            workingForeground = current?.foreground ?: original.foreground
            workingBackground = current?.background ?: original.background
            _state.value = MaskEditorUiState(
                loading = false,
                ready = true,
                dirty = current != null,
                workingForeground = workingForeground
            )
        }
    }

    fun expand(radius: Int) = apply(radius) { a, w, h, r -> MaskOps.expand(a, w, h, r) }

    fun shrink(radius: Int) = apply(radius) { a, w, h, r -> MaskOps.shrink(a, w, h, r) }

    fun feather(radius: Int) = apply(radius) { a, w, h, r -> MaskOps.feather(a, w, h, r) }

    fun smooth(radius: Int) = apply(radius) { a, w, h, r -> MaskOps.smooth(a, w, h, r) }

    /** Applies an alpha operation to the working mask and re-derives the background hole. */
    private fun apply(radius: Int, op: (ByteArray, Int, Int, Int) -> ByteArray) {
        val foreground = workingForeground ?: return
        val source = sourceBitmap ?: return
        if (radius <= 0) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val (newForeground, newBackground) = withContext(Dispatchers.Default) {
                val alpha = MaskMath.alphaOf(foreground)
                val newAlpha = op(alpha, foreground.width, foreground.height, radius)
                val newForeground = MaskMath.withAlpha(foreground, newAlpha)
                val newBackground = MaskMath.eraseSubject(source, newForeground)
                newForeground to newBackground
            }
            workingForeground = newForeground
            workingBackground = newBackground
            _state.update {
                it.copy(busy = false, dirty = true, workingForeground = newForeground)
            }
        }
    }

    /** Restores the unedited AI mask. Nothing is persisted until [save]. */
    fun reset() {
        val foreground = originalForeground ?: return
        val background = originalBackground ?: return
        workingForeground = foreground
        workingBackground = background
        _state.update { it.copy(busy = false, dirty = false, workingForeground = foreground) }
    }

    /**
     * Persists the working mask (or clears a previous edit when the user reset to
     * the AI mask), then signals the wallpaper service to reload. [onSaved] runs
     * on the main thread once the disk write completes.
     */
    fun save(onSaved: () -> Unit) {
        val k = key ?: return
        val foreground = workingForeground ?: return
        val background = workingBackground ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (_state.value.dirty) {
                    store.saveEdited(k, SegmentationResult(foreground, background))
                } else {
                    store.deleteEdited(k)
                }
            }
            notifier.emit(Unit)
            onSaved()
        }
    }
}
