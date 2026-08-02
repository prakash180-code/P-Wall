package com.prakash.pwall.service.depth

import java.security.MessageDigest

/**
 * Stable identifiers for cached masks, derived from the wallpaper image's identity.
 */
object MaskKeys {

    /**
     * Deterministic key for an image file. Combines the path and the file's
     * last-modified time so a replaced image produces a new cache entry.
     */
    fun forImage(path: String, lastModified: Long): String =
        sha256("$path:$lastModified").take(24)

    /** Discriminator used to tag the edited-mask that Prompt 4's manual editor will write. */
    const val EDITED = "edited"

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
