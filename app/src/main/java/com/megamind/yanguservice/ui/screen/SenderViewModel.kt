package com.megamind.yanguservice.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megamind.yanguservice.domain.AuthTokenStore
import com.megamind.yanguservice.domain.ImageAttachment
import com.megamind.yanguservice.domain.ImageContentReader
import com.megamind.yanguservice.domain.SenderRepository
import com.megamind.yanguservice.utlis.BulkSendResult
import com.megamind.yanguservice.utlis.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SenderUiState(
    val isApiTokenConfigured: Boolean = false,
    val isSavingToken: Boolean = false,
    val isSending: Boolean = false,
    val isImageLoading: Boolean = false,
    val selectedImageUri: String? = null,
    val selectedImageName: String? = null,
    val result: BulkSendResult? = null,
    val error: String? = null
)

class SenderViewModel(
    private val repository: SenderRepository,
    private val imageContentReader: ImageContentReader,
    private val authTokenStore: AuthTokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SenderUiState(isApiTokenConfigured = !authTokenStore.getToken().isNullOrBlank())
    )
    val uiState = _uiState.asStateFlow()

    private var selectedImage: ImageAttachment? = null
    private var imageLoadJob: Job? = null
    private var sendJob: Job? = null

    fun saveApiToken(token: String) {
        if (_uiState.value.isSavingToken || token.isBlank()) {
            if (token.isBlank()) {
                _uiState.update { it.copy(error = "Saisissez un token API valide") }
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingToken = true, error = null) }
            when (val result = authTokenStore.saveToken(token)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isApiTokenConfigured = true,
                            isSavingToken = false,
                            error = null
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSavingToken = false,
                            error = result.e?.message ?: "Impossible d'enregistrer le token API"
                        )
                    }
                }
            }
        }
    }

    fun clearApiToken() {
        if (_uiState.value.isSending) return
        viewModelScope.launch {
            authTokenStore.clearToken()
            _uiState.update {
                it.copy(
                    isApiTokenConfigured = false,
                    isSavingToken = false,
                    error = null
                )
            }
        }
    }

    fun selectImage(uri: String) {
        if (_uiState.value.isSending) return

        imageLoadJob?.cancel()
        selectedImage = null
        imageLoadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImageLoading = true,
                    selectedImageUri = null,
                    selectedImageName = null,
                    result = null,
                    error = null
                )
            }

            when (val result = imageContentReader.read(uri)) {
                is Result.Success -> {
                    val image = result.data
                    if (image == null) {
                        _uiState.update {
                            it.copy(
                                isImageLoading = false,
                                error = "Impossible de charger l'image sélectionnée"
                            )
                        }
                    } else {
                        selectedImage = image
                        _uiState.update {
                            it.copy(
                                isImageLoading = false,
                                selectedImageUri = uri,
                                selectedImageName = image.fileName
                            )
                        }
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isImageLoading = false,
                            error = result.e?.message ?: "Impossible de charger l'image"
                        )
                    }
                }
            }
        }
    }

    fun removeImage() {
        if (_uiState.value.isSending) return
        imageLoadJob?.cancel()
        selectedImage = null
        _uiState.update {
            it.copy(
                isImageLoading = false,
                selectedImageUri = null,
                selectedImageName = null,
                result = null,
                error = null
            )
        }
    }

    fun sendToMany(message: String, phoneNumbers: List<String>) {
        if (_uiState.value.isSending || _uiState.value.isImageLoading) return
        if (!_uiState.value.isApiTokenConfigured) {
            _uiState.update { it.copy(error = "Configurez d'abord le token API") }
            return
        }
        if (phoneNumbers.isEmpty()) {
            _uiState.update { it.copy(error = "Ajoutez au moins un destinataire") }
            return
        }
        if (message.isBlank() && selectedImage == null) {
            _uiState.update { it.copy(error = "Ajoutez un message ou une image") }
            return
        }

        sendJob = viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, result = null, error = null) }
            try {
                val result = selectedImage?.let { image ->
                    repository.sendImageToMany(message.trim(), phoneNumbers, image)
                } ?: repository.sendToMany(message.trim(), phoneNumbers)

                _uiState.update { it.copy(isSending = false, result = result) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        error = exception.message ?: "Une erreur est survenue pendant l'envoi"
                    )
                }
            }
        }
    }

    fun cancel() {
        sendJob?.cancel()
        _uiState.update { it.copy(isSending = false) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
