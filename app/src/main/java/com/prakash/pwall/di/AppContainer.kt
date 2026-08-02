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

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext.settingsDataStore, imageStore)
    }

    /** The file the live wallpaper service reads. */
    val wallpaperImagePath: String?
        get() = imageStore.imagePath
}
