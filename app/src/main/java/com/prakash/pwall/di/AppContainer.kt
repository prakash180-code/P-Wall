package com.prakash.pwall.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.prakash.pwall.data.repository.SettingsRepository
import com.prakash.pwall.data.storage.ImageStore
import com.prakash.pwall.license.LicenseManager
import com.prakash.pwall.license.OfflineLicenseManager
import com.prakash.pwall.service.depth.DiskMaskStore
import kotlinx.coroutines.flow.MutableSharedFlow

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pwall_settings"
)

/**
 * Lightweight manual dependency container (constructor injection).
 * Owned by [PWallApplication] and exposed to Compose via a composition local.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val licenseManager: LicenseManager = OfflineLicenseManager()

    val imageStore: ImageStore = ImageStore(appContext)

    /** Stores the custom image for the time widget container (kept separate). */
    val widgetImageStore: ImageStore = ImageStore(appContext, "widget_images", "widget_image")

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext.settingsDataStore, imageStore, widgetImageStore)
    }

    /** Shared mask cache, read by the wallpaper service and the manual depth editor. */
    val maskStore: DiskMaskStore by lazy { DiskMaskStore(appContext) }

    /**
     * One-shot signal that the manual depth editor saved or reset a mask, so the
     * live wallpaper service reloads it for the current image immediately.
     */
    val maskEditNotifier: MutableSharedFlow<Unit> = MutableSharedFlow(extraBufferCapacity = 1)

    /** The file the live wallpaper service reads. */
    val wallpaperImagePath: String?
        get() = imageStore.imagePath
}
