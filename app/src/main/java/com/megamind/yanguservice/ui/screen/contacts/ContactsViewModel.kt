package com.megamind.yanguservice.ui.screen.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megamind.yanguservice.domain.model.Contact
import com.megamind.yanguservice.domain.model.ContactImportResult
import com.megamind.yanguservice.domain.repo.ContactRepository
import com.megamind.yanguservice.domain.usecase.ImportVcfContacts
import com.megamind.yanguservice.domain.utils.normalizeInternationalNumber
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
    val isImporting: Boolean = false,
    val importResult: ContactImportResult? = null,
    val selectedContactIds: Set<String> = emptySet(),
)

class ContactsViewModel(
    private val repository: ContactRepository,
    private val importVcfContacts: ImportVcfContacts,
) : ViewModel() {
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
                    val existingIds = contacts.mapTo(HashSet()) { it.id }
                    _uiState.update {
                        it.copy(
                            contacts = contacts,
                            selectedContactIds = it.selectedContactIds.intersect(existingIds),
                            isLoading = false,
                        )
                    }
                }
        }
    }

    fun toggleContactSelection(id: String) {
        _uiState.update { state ->
            if (state.contacts.none { it.id == id }) state
            else state.copy(
                selectedContactIds = if (id in state.selectedContactIds) {
                    state.selectedContactIds - id
                } else {
                    state.selectedContactIds + id
                },
            )
        }
    }

    fun toggleSelectAll() {
        _uiState.update { state ->
            val allIds = state.contacts.mapTo(HashSet()) { it.id }
            state.copy(
                selectedContactIds = if (state.selectedContactIds.containsAll(allIds)) {
                    emptySet()
                } else {
                    allIds
                },
            )
        }
    }

    fun selectedContacts(): List<Contact> = _uiState.value.let { state ->
        state.contacts.filter { it.id in state.selectedContactIds }
    }

    fun openAddSheet() {
        if (_uiState.value.isImporting) return
        _uiState.update { it.copy(isAddSheetOpen = true, error = null) }
    }

    fun dismissAddSheet() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isAddSheetOpen = false, error = null) }
    }

    fun addContact(name: String, phoneNumber: String) {
        if (_uiState.value.isSaving) return

        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            _uiState.update { it.copy(error = "Saisissez un nom") }
            return
        }
        val cleanPhoneNumber = normalizeInternationalNumber(phoneNumber) ?: run {
            _uiState.update {
                it.copy(error = "Saisissez un numéro international valide, par exemple +33612345678")
            }
            return
        }

        saveContact(Contact(UUID.randomUUID().toString(), cleanName, cleanPhoneNumber))
    }

    fun importVcf(uri: String) {
        if (_uiState.value.isImporting || _uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importResult = null, error = null) }
            try {
                val result = importVcfContacts(uri)
                _uiState.update { it.copy(isImporting = false, importResult = result) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        error = error.message ?: "Impossible d'importer le fichier VCF",
                    )
                }
            }
        }
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

}
