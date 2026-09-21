package com.megamind.yanguservice.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megamind.yanguservice.domain.utils.AuthTokenStore
import com.megamind.yanguservice.utlis.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isApiTokenConfigured: Boolean = false,
    val isSavingToken: Boolean = false,
    val error: String? = null
)

class SettingsViewModel(
    private val authTokenStore: AuthTokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(isApiTokenConfigured = !authTokenStore.getToken().isNullOrBlank())
    )
    val uiState = _uiState.asStateFlow()

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
        if (_uiState.value.isSavingToken) return

        viewModelScope.launch {
            authTokenStore.clearToken()
            _uiState.update {
                it.copy(
                    isApiTokenConfigured = false,
                    error = null
                )
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
