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
