package com.prakash.pwall.license

/**
 * Abstraction for licensing / activation.
 *
 * All licensing checks across the app must go through this interface so the
 * implementation can be swapped later (e.g. Supabase-backed activation)
 * without touching the rest of the application.
 */
interface LicenseManager {

    /** Whether the current install is activated and allowed to run. */
    fun isActivated(): Boolean
}
