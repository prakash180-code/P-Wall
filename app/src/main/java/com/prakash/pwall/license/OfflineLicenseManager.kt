package com.prakash.pwall.license

/**
 * Default implementation: every install is considered authorized.
 *
 * This keeps the app fully offline. A future `OnlineLicenseManager` backed by
 * Supabase can replace this without changing any other code.
 */
class OfflineLicenseManager : LicenseManager {

    override fun isActivated(): Boolean = true
}
