package com.prakash.pwall.utils

import android.util.Log
import com.prakash.pwall.BuildConfig

/**
 * Central logging for P-Wall. Verbose/diagnostic messages are stripped by R8 in
 * release builds (BuildConfig.DEBUG is a compile-time constant), so per-frame
 * debug logging costs nothing in production.
 */
object PWallLog {

    private const val TAG = "P-Wall"

    /** Diagnostic detail, only in debug builds. */
    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    /** Important lifecycle/state transitions. */
    fun i(message: String) = Log.i(TAG, message)

    /** Recoverable problems (a dropped layer, a skipped frame mode). */
    fun w(message: String) = Log.w(TAG, message)

    /** Failures that still should not kill the wallpaper. */
    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e(TAG, message, throwable) else Log.e(TAG, message)
    }
}
