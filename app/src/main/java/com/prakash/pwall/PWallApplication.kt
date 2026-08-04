package com.prakash.pwall

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import com.prakash.pwall.di.AppContainer
import com.prakash.pwall.utils.BitmapCache

class PWallApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        val am = getSystemService(ActivityManager::class.java)
        BitmapCache.configureForMemoryClass(am.memoryClass)
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> BitmapCache.trim()

            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> BitmapCache.clear()

            // Foreground app simply moved to background; nothing to release yet.
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> Unit
        }
    }
}
