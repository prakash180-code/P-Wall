package com.prakash.pwall.service.depth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskKeysTest {

    @Test
    fun forImage_isDeterministic() {
        val a = MaskKeys.forImage("/wallpaper/a.jpg", 1000L)
        val b = MaskKeys.forImage("/wallpaper/a.jpg", 1000L)
        assertEquals(a, b)
    }

    @Test
    fun forImage_changesWhenFileModified() {
        val a = MaskKeys.forImage("/wallpaper/a.jpg", 1000L)
        val b = MaskKeys.forImage("/wallpaper/a.jpg", 2000L)
        assertNotEquals(a, b)
    }

    @Test
    fun forImage_changesWhenPathChanges() {
        val a = MaskKeys.forImage("/wallpaper/a.jpg", 1000L)
        val b = MaskKeys.forImage("/wallpaper/b.jpg", 1000L)
        assertNotEquals(a, b)
    }

    @Test
    fun forImage_isStableLength() {
        assertEquals(24, MaskKeys.forImage("/wallpaper/a.jpg", 1000L).length)
        assertEquals(24, MaskKeys.forImage("/some/very/long/path/with/a/huge/name.png", 999999L).length)
    }

    @Test
    fun forImage_isHex() {
        assertTrue(MaskKeys.forImage("/wallpaper/a.jpg", 1000L).all { it in "0123456789abcdef" })
    }

    @Test
    fun edited_discriminatorIsStable() {
        assertEquals("edited", MaskKeys.EDITED)
    }
}
