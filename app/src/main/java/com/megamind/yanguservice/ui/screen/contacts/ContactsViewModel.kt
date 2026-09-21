package com.megamind.yanguservice.ui.screen.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megamind.yanguservice.domain.model.Contact
import com.megamind.yanguservice.domain.repo.ContactRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = true,
    val isAddSheetOpen: Boolean = false,
    val isSaving: Boolean = false,
)

class ContactsViewModel(private val repository: ContactRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeContacts()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Impossible de charger les contacts",
                        )
                    }
                }
                .collect { contacts ->
                    _uiState.update { it.copy(contacts = contacts, isLoading = false) }
                }
        }
    }

    fun openAddSheet() {
        _uiState.update { it.copy(isAddSheetOpen = true, error = null) }
    }

    fun dismissAddSheet() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isAddSheetOpen = false, error = null) }
    }

    fun addContact(name: String, phoneNumber: String) {
        if (_uiState.value.isSaving) return

        val cleanName = name.trim()
        val cleanPhoneNumber = phoneNumber.filterNot(Char::isWhitespace).trim()
        val error = when {
            cleanName.isEmpty() -> "Saisissez un nom"
            !PHONE_NUMBER_PATTERN.matches(cleanPhoneNumber) ->
                "Saisissez un numéro international valide, par exemple +33612345678"
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }

        saveContact(Contact(UUID.randomUUID().toString(), cleanName, cleanPhoneNumber))
    }

    fun saveContact(contact: Contact) = viewModelScope.launch {
        if (_uiState.value.isSaving) return@launch
        _uiState.update { it.copy(isSaving = true, error = null) }
        try {
            repository.saveContact(contact)
            _uiState.update {
                it.copy(isSaving = false, isAddSheetOpen = false, error = null)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _uiState.update {
                it.copy(isSaving = false, error = error.message ?: "Impossible d'enregistrer le contact")
            }
        }
    }

    fun deleteContact(id: String) = viewModelScope.launch {
        try {
            repository.deleteContact(id)
            _uiState.update { it.copy(error = null) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _uiState.update {
                it.copy(error = error.message ?: "Impossible de supprimer le contact")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private companion object {
        val PHONE_NUMBER_PATTERN = Regex("^\\+[1-9]\\d{7,14}$")
    }
}
