package com.prakash.pwall.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.repository.SettingsRepository
import com.prakash.pwall.data.storage.ImageStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val imageStore: ImageStore
) : ViewModel() {

    data class HomeUiState(
        val settings: WallpaperSettings = WallpaperSettings(),
        val isSavingImage: Boolean = false,
        val message: String? = null
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onImageSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingImage = true, message = null) }
            runCatching { imageStore.saveImage(uri) }
                .onSuccess { path ->
                    settingsRepository.updateSettings { it.copy(selectedImagePath = path) }
                    _uiState.update {
                        it.copy(isSavingImage = false, message = "Image selected")
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingImage = false,
                            message = error.message ?: "Could not load that image"
                        )
                    }
                }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
