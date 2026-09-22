package com.megamind.yanguservice.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megamind.yanguservice.domain.model.Contact
import com.megamind.yanguservice.domain.utils.AuthTokenStore
import com.megamind.yanguservice.domain.utils.ImageAttachment
import com.megamind.yanguservice.domain.utils.ImageContentReader
import com.megamind.yanguservice.domain.repo.SenderRepository
import com.megamind.yanguservice.utlis.BulkSendResult
import com.megamind.yanguservice.utlis.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SenderUiState(
    val message: String = "",
    val recipientsInput: String = "",
    val selectedContacts: List<Contact> = emptyList(),
    val isApiTokenConfigured: Boolean = false,
    val isSending: Boolean = false,
    val isImageLoading: Boolean = false,
    val selectedImageUri: String? = null,
    val selectedImageName: String? = null,
    val result: BulkSendResult? = null,
    val error: String? = null
) {
    val phoneNumbers: List<String>
        get() = (selectedContacts.map(Contact::phoneNumber) + recipientsInput.toPhoneNumbers())
            .distinct()

    fun canSend(): Boolean =
        isApiTokenConfigured &&
                !isSending &&
                !isImageLoading &&
                phoneNumbers.isNotEmpty() &&
                (message.isNotBlank() || selectedImageUri != null)
}

private fun String.toPhoneNumbers(): List<String> =
    split(Regex("[,;\\n]+"))
        .map(String::trim)
        .filter(String::isNotEmpty)

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

    fun refreshApiTokenConfiguration() {
        val isConfigured = !authTokenStore.getToken().isNullOrBlank()
        if (_uiState.value.isApiTokenConfigured != isConfigured) {
            _uiState.update { it.copy(isApiTokenConfigured = isConfigured) }
        }
    }

    fun updateMessage(message: String) {
        if (_uiState.value.isSending) return
        _uiState.update { it.copy(message = message, result = null, error = null) }
    }

    fun updateRecipientsInput(recipientsInput: String) {
        if (_uiState.value.isSending) return
        _uiState.update { it.copy(recipientsInput = recipientsInput, result = null, error = null) }
    }

    fun setSelectedContacts(contacts: List<Contact>) {
        if (_uiState.value.isSending) return
        _uiState.update {
            it.copy(
                selectedContacts = contacts.distinctBy(Contact::id),
                result = null,
                error = null
            )
        }
    }

    fun removeContact(contact: Contact) {
        if (_uiState.value.isSending) return
        _uiState.update {
            it.copy(
                selectedContacts = it.selectedContacts.filterNot { selected -> selected.id == contact.id },
                result = null,
                error = null
            )
        }
    }

    fun selectImage(uri: String) {
        if (_uiState.value.isSending) return

        imageLoadJob?.cancel()
        selectedImage = null
        _uiState.update {
            it.copy(
                isImageLoading = true,
                selectedImageUri = null,
                selectedImageName = null,
                result = null,
                error = null
            )
        }
        imageLoadJob = viewModelScope.launch {
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



    fun sendToMany() {
        val state = _uiState.value
        if (state.isSending || state.isImageLoading) return
        if (!state.isApiTokenConfigured) {
            _uiState.update { it.copy(error = "Configurez d'abord le token API") }
            return
        }
        val phoneNumbers = state.phoneNumbers
        if (phoneNumbers.isEmpty()) {
            _uiState.update { it.copy(error = "Ajoutez au moins un destinataire") }
            return
        }
        if (state.message.isBlank() && selectedImage == null) {
            _uiState.update { it.copy(error = "Ajoutez un message ou une image") }
            return
        }

        _uiState.update { it.copy(isSending = true, result = null, error = null) }
        sendJob = viewModelScope.launch {
            try {
                val result = selectedImage?.let { image ->
                    repository.sendImageToMany(state.message.trim(), phoneNumbers, image)
                } ?: repository.sendToMany(state.message.trim(), phoneNumbers)

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
