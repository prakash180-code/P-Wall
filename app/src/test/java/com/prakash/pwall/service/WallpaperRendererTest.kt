package com.prakash.pwall.service

import com.prakash.pwall.data.model.BackgroundMode
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.WallpaperSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperRendererTest {

    private val settings = WallpaperSettings()

    @Test
    fun backgroundTransform_fit_keepsAspectRatio() {
        val t = WallpaperRenderer.backgroundTransform(200, 100, 100, 100, BackgroundMode.FIT)
        // scale = 0.5, centered vertically at y=25
        assertEquals(0.5f, t.scaleX, 0.001f)
        assertEquals(0.5f, t.scaleY, 0.001f)
        assertEquals(0f, t.offsetX, 0.001f)
        assertEquals(25f, t.offsetY, 0.001f)
    }

    @Test
    fun backgroundTransform_fill_cropsToTarget() {
        val t = WallpaperRenderer.backgroundTransform(100, 200, 100, 100, BackgroundMode.FILL)
        // scale = 1.0, centered vertically at y=-50
        assertEquals(1f, t.scaleX, 0.001f)
        assertEquals(1f, t.scaleY, 0.001f)
        assertEquals(0f, t.offsetX, 0.001f)
        assertEquals(-50f, t.offsetY, 0.001f)
    }

    @Test
    fun backgroundTransform_stretch_mapsToTargetExactly() {
        val t = WallpaperRenderer.backgroundTransform(200, 100, 100, 100, BackgroundMode.STRETCH)
        assertEquals(0.5f, t.scaleX, 0.001f)
        assertEquals(1f, t.scaleY, 0.001f)
        assertEquals(0f, t.offsetX, 0.001f)
        assertEquals(0f, t.offsetY, 0.001f)
    }

    @Test
    fun customPanBounds_zeroRotation_atCoverScale_onlyOverflowAxisHasRoom() {
        // 200x100 image in a 100x100 target at zoom 1: cover scale = 1.0,
        // so the scaled image is 200x100 -> 50px horizontal overflow each
        // side, none vertically.
        val (maxPanX, maxPanY) = WallpaperRenderer.customPanBounds(
            200, 100, 100, 100, zoom = 1f, rotationDegrees = 0f
        )
        assertEquals(50f, maxPanX, 0.001f)
        assertEquals(0f, maxPanY, 0.001f)
    }

    @Test
    fun customPanBounds_zoomTwo_addsPanRoom() {
        // 200x100 image, 100x100 target, zoom 2: cover scale = 1.0, scaled to
        // 400x200, centered => 150px overflow horizontally, 50px vertically.
        val (maxPanX, maxPanY) = WallpaperRenderer.customPanBounds(
            200, 100, 100, 100, zoom = 2f, rotationDegrees = 0f
        )
        assertEquals(150f, maxPanX, 0.001f)
        assertEquals(50f, maxPanY, 0.001f)
    }

    @Test
    fun customPanBounds_zoomBelowOneIsClampedToCover() {
        // zoom 0.5 still clamps to cover (scale >= 1), matching zoom 1 bounds.
        val (maxPanX, maxPanY) = WallpaperRenderer.customPanBounds(
            200, 100, 100, 100, zoom = 0.5f, rotationDegrees = 0f
        )
        assertEquals(50f, maxPanX, 0.001f)
        assertEquals(0f, maxPanY, 0.001f)
    }

    @Test
    fun customPanBounds_rotationNeverShrinksRoom() {
        val unrotated = WallpaperRenderer.customPanBounds(
            200, 100, 100, 100, zoom = 2f, rotationDegrees = 0f
        )
        val rotated = WallpaperRenderer.customPanBounds(
            200, 100, 100, 100, zoom = 2f, rotationDegrees = 30f
        )
        // Rotating widens the bounding box, so pan room only grows.
        assert(rotated.first >= unrotated.first)
        assert(rotated.second >= unrotated.second)
    }

    @Test
    fun blockTopLeft_bottomCenterIsCenteredHorizontally() {
        val (x, y) = WallpaperRenderer.blockTopLeft(
            canvasWidth = 1000f,
            canvasHeight = 2000f,
            blockWidth = 300f,
            blockHeight = 100f,
            settings = settings.copy(position = PositionPreset.BOTTOM_CENTER)
        )
        assertEquals(350f, x, 0.001f)
        assertEquals(2000f - 28f - 100f, y, 0.001f)
    }

    @Test
    fun blockTopLeft_topLeftUsesPadding() {
        val (x, y) = WallpaperRenderer.blockTopLeft(
            canvasWidth = 1000f,
            canvasHeight = 2000f,
            blockWidth = 300f,
            blockHeight = 100f,
            settings = settings.copy(position = PositionPreset.TOP_LEFT)
        )
        assertEquals(28f, x, 0.001f)
        assertEquals(28f, y, 0.001f)
    }

    @Test
    fun backgroundPanBounds_fill_matchesTransformOverflow() {
        // 200x100 image in 100x100 target, FILL scale = max(0.5, 1.0) = 1.0,
        // scaled image is 200x100 => 50px horizontal overflow, none vertically.
        val (maxPanX, maxPanY) = WallpaperRenderer.backgroundPanBounds(
            200, 100, 100, 100,
            settings.copy(backgroundMode = BackgroundMode.FILL)
        )
        assertEquals(50f, maxPanX, 0.001f)
        assertEquals(0f, maxPanY, 0.001f)
    }

    @Test
    fun backgroundPanBounds_fit_neverHasRoom() {
        // FIT never overflows (the whole image is visible), so pan room is zero.
        val (maxPanX, maxPanY) = WallpaperRenderer.backgroundPanBounds(
            200, 100, 100, 100,
            settings.copy(backgroundMode = BackgroundMode.FIT)
        )
        assertEquals(0f, maxPanX, 0.001f)
        assertEquals(0f, maxPanY, 0.001f)
    }

    @Test
    fun backgroundPanBounds_custom_delegatesToCustomPanBounds() {
        val (maxPanX, maxPanY) = WallpaperRenderer.backgroundPanBounds(
            200, 100, 100, 100,
            settings.copy(
                backgroundMode = BackgroundMode.CUSTOM,
                backgroundZoom = 2f,
                backgroundRotationDegrees = 0f
            )
        )
        assertEquals(150f, maxPanX, 0.001f)
        assertEquals(50f, maxPanY, 0.001f)
    }

    @Test
    fun blockTopLeft_customCentersOnFraction() {
        val (x, y) = WallpaperRenderer.blockTopLeft(
            canvasWidth = 1000f,
            canvasHeight = 2000f,
            blockWidth = 300f,
            blockHeight = 100f,
            settings = settings.copy(
                position = PositionPreset.CUSTOM,
                positionXFraction = 0.5f,
                positionYFraction = 0.5f
            )
        )
        assertEquals(500f - 150f, x, 0.001f)
        assertEquals(1000f - 50f, y, 0.001f)
    }
}
