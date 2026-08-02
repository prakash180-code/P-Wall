package com.prakash.pwall.license

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineLicenseManagerTest {

    private val licenseManager = OfflineLicenseManager()

    @Test
    fun offlineManagerAlwaysActivated() {
        assertTrue(licenseManager.isActivated())
    }

    @Test
    fun interfaceIsImplemented() {
        val manager: LicenseManager = licenseManager
        assertFalse(!manager.isActivated())
    }
}
