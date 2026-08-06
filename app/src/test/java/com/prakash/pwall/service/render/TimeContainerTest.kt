package com.prakash.pwall.service.render

import com.prakash.pwall.data.model.BackgroundMode
import com.prakash.pwall.data.model.ContainerBorderStyle
import com.prakash.pwall.data.model.ContainerShape
import com.prakash.pwall.data.model.GradientDirection
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.model.WidgetBackgroundMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeContainerTest {

    @Test
    fun isInactive_trueOnlyForTransparent() {
        assertTrue(TimeContainer.isInactive(WallpaperSettings(widgetBackgroundMode = WidgetBackgroundMode.TRANSPARENT)))
        assertFalse(TimeContainer.isInactive(WallpaperSettings(widgetBackgroundMode = WidgetBackgroundMode.SOLID)))
        assertFalse(TimeContainer.isInactive(WallpaperSettings(widgetBackgroundMode = WidgetBackgroundMode.GRADIENT)))
        assertFalse(TimeContainer.isInactive(WallpaperSettings(widgetBackgroundMode = WidgetBackgroundMode.IMAGE)))
    }

    @Test
    fun fillAlpha_mapsPercentToFraction() {
        assertEquals(0f, TimeContainer.fillAlpha(WallpaperSettings(widgetBackgroundOpacity = 0)), 0f)
        assertEquals(0.5f, TimeContainer.fillAlpha(WallpaperSettings(widgetBackgroundOpacity = 50)), 1e-6f)
        assertEquals(1f, TimeContainer.fillAlpha(WallpaperSettings(widgetBackgroundOpacity = 100)), 1e-6f)
        // Out-of-range values are clamped.
        assertEquals(0f, TimeContainer.fillAlpha(WallpaperSettings(widgetBackgroundOpacity = -5)), 0f)
        assertEquals(1f, TimeContainer.fillAlpha(WallpaperSettings(widgetBackgroundOpacity = 120)), 0f)
    }

    @Test
    fun geometry_padsTextBoxByUserPaddingScaledByDensity() {
        val settings = WallpaperSettings(
            widgetPaddingHorizontal = 20f,
            widgetPaddingVertical = 14f
        )
        val rect = TimeContainer.geometry(100f, 50f, 200f, 40f, settings, density = 2f)

        assertEquals(100f - 40f, rect.left, 1e-4f)
        assertEquals(50f - 28f, rect.top, 1e-4f)
        assertEquals(100f + 200f + 40f, rect.right, 1e-4f)
        assertEquals(50f + 40f + 28f, rect.bottom, 1e-4f)
    }

    @Test
    fun cornerRadius_sharpShapes_areZero() {
        assertEquals(
            0f,
            TimeContainer.cornerRadius(ContainerShape.NONE, 24f, 1f, 400f, 80f),
            0f
        )
        assertEquals(
            0f,
            TimeContainer.cornerRadius(ContainerShape.RECTANGLE, 24f, 1f, 400f, 80f),
            0f
        )
    }

    @Test
    fun cornerRadius_roundedScalesWithDensityAndClamps() {
        assertEquals(
            48f,
            TimeContainer.cornerRadius(ContainerShape.ROUNDED, 24f, 2f, 400f, 80f),
            1e-4f
        )
        // Radius is clamped to a 100dp ceiling.
        assertEquals(
            100f,
            TimeContainer.cornerRadius(ContainerShape.ROUNDED, 200f, 1f, 400f, 80f),
            1e-4f
        )
    }

    @Test
    fun imageDrawRect_fitLetterboxesInsideContainer() {
        val container = TimeContainer.ContainerRect(0f, 0f, 400f, 100f)
        val rect = TimeContainer.imageDrawRect(2000, 1000, container, BackgroundMode.FIT)
        requireNotNull(rect)
        // 2:1 source into a 4:1 box -> width limited by height.
        assertEquals(100f, rect.height, 1e-4f)
        assertTrue(rect.left >= container.left - 1e-4f)
        assertTrue(rect.right <= container.right + 1e-4f)
    }

    @Test
    fun imageDrawRect_centerCropCoversContainer() {
        val container = TimeContainer.ContainerRect(0f, 0f, 400f, 100f)
        val rect = TimeContainer.imageDrawRect(1000, 1000, container, BackgroundMode.CENTER_CROP)
        requireNotNull(rect)
        assertEquals(400f, rect.width, 1e-4f)
        assertTrue(rect.height >= 100f - 1e-4f)
    }

    @Test
    fun imageDrawRect_stretchFillsExactly() {
        val container = TimeContainer.ContainerRect(10f, 20f, 410f, 120f)
        val rect = TimeContainer.imageDrawRect(7, 3, container, BackgroundMode.STRETCH)
        assertEquals(container, rect)
    }

    @Test
    fun imageDrawRect_emptySourceOrContainer_returnsNull() {
        val container = TimeContainer.ContainerRect(0f, 0f, 400f, 100f)
        assertNull(TimeContainer.imageDrawRect(0, 100, container, BackgroundMode.FIT))
        assertNull(TimeContainer.imageDrawRect(100, 100, TimeContainer.ContainerRect(0f, 0f, 0f, 0f), BackgroundMode.FIT))
    }

    @Test
    fun gradientEndpoints_followDirection() {
        val rect = TimeContainer.ContainerRect(10f, 20f, 110f, 120f)
        val (start, end) = TimeContainer.gradientEndpoints(GradientDirection.LEFT_TO_RIGHT, rect)
        assertEquals(10f, start.first, 0f)
        assertEquals(110f, end.first, 0f)
        assertEquals(20f, start.second, 0f)

        val (dStart, dEnd) = TimeContainer.gradientEndpoints(GradientDirection.DIAGONAL, rect)
        assertEquals(10f, dStart.first, 0f)
        assertEquals(120f, dStart.second, 0f)
        assertEquals(110f, dEnd.first, 0f)
        assertEquals(20f, dEnd.second, 0f)
    }

    @Test
    fun borderDash_solidIsNull_dashedAndDottedScaleWithWidth() {
        assertNull(TimeContainer.borderDash(ContainerBorderStyle.SOLID, 2f))
        assertArrayEquals(
            floatArrayOf(4f, 3f),
            TimeContainer.borderDash(ContainerBorderStyle.DASHED, 2f),
            1e-6f
        )
        assertArrayEquals(
            floatArrayOf(2f, 3f),
            TimeContainer.borderDash(ContainerBorderStyle.DOTTED, 2f),
            1e-6f
        )
    }

    @Test
    fun containerRect_inflatedAndShifted() {
        val rect = TimeContainer.ContainerRect(0f, 0f, 100f, 50f)
        val inflated = rect.inflated(10f)
        assertEquals(-10f, inflated.left, 0f)
        assertEquals(110f, inflated.right, 0f)

        val shifted = rect.shifted(5f, -5f)
        assertEquals(5f, shifted.left, 0f)
        assertEquals(-5f, shifted.top, 0f)
    }
}
