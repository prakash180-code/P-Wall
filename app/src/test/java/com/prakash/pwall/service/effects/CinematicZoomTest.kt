package com.prakash.pwall.service.effects

import com.prakash.pwall.data.model.ZoomDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class CinematicZoomTest {

    @Test
    fun zeroStrength_returnsIdentity() {
        assertEquals(1f, CinematicZoom.zoomAt(5_000L, 30_000L, 0f, ZoomDirection.ZOOM_IN), 0.0001f)
        assertEquals(1f, CinematicZoom.zoomAt(5_000L, 0L, 0.5f, ZoomDirection.ZOOM_IN), 0.0001f)
        assertEquals(1f, CinematicZoom.zoomAt(5_000L, 30_000L, -1f, ZoomDirection.ZOOM_IN), 0.0001f)
    }

    @Test
    fun zoomIn_startsAtIdentityAndGrows() {
        val strength = 0.5f
        val duration = 30_000L
        assertEquals(1f, CinematicZoom.zoomAt(0L, duration, strength, ZoomDirection.ZOOM_IN), 0.0001f)
        assertEquals(
            1f + strength * 0.5f,
            CinematicZoom.zoomAt(duration / 2, duration, strength, ZoomDirection.ZOOM_IN),
            0.0001f
        )
        assertEquals(
            1f + strength,
            CinematicZoom.zoomAt(duration - 1L, duration, strength, ZoomDirection.ZOOM_IN),
            0.0001f
        )
    }

    @Test
    fun zoomOut_startsAtMaxAndShrinks() {
        val strength = 0.25f
        val duration = 10_000L
        assertEquals(
            1f + strength,
            CinematicZoom.zoomAt(0L, duration, strength, ZoomDirection.ZOOM_OUT),
            0.0001f
        )
        assertEquals(
            1f + strength * 0.5f,
            CinematicZoom.zoomAt(duration / 2, duration, strength, ZoomDirection.ZOOM_OUT),
            0.0001f
        )
        assertEquals(1f, CinematicZoom.zoomAt(duration - 1L, duration, strength, ZoomDirection.ZOOM_OUT), 0.0001f)
    }

    @Test
    fun alternate_sweepsInAndOutAndWraps() {
        val strength = 0.5f
        val duration = 10_000L
        assertEquals(1f, CinematicZoom.zoomAt(0L, duration, strength, ZoomDirection.ALTERNATE), 0.0001f)
        assertEquals(
            1f + strength / 2f,
            CinematicZoom.zoomAt(duration / 4, duration, strength, ZoomDirection.ALTERNATE),
            0.0001f
        )
        assertEquals(
            1f + strength,
            CinematicZoom.zoomAt(duration / 2, duration, strength, ZoomDirection.ALTERNATE),
            0.0001f
        )
        // Loops back around cleanly after the duration passes.
        assertEquals(1f, CinematicZoom.zoomAt(duration, duration, strength, ZoomDirection.ALTERNATE), 0.0001f)
        assertEquals(
            1f + strength / 2f,
            CinematicZoom.zoomAt(duration + duration / 4, duration, strength, ZoomDirection.ALTERNATE),
            0.0001f
        )
    }
}
