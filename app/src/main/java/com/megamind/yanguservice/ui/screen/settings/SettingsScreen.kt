package com.megamind.yanguservice.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.megamind.yanguservice.ui.theme.YanguServiceTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreenContent(
        uiState = uiState,
        onSaveApiToken = viewModel::saveApiToken,
        onClearApiToken = viewModel::clearApiToken,
        onClearError = viewModel::clearError,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onSaveApiToken: (String) -> Unit,
    onClearApiToken: () -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var token by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isApiTokenConfigured) {
        if (uiState.isApiTokenConfigured) token = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("Retour")
        }
        Text(
            text = "Paramètres",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Configuration de l'API Wasender",
            style = MaterialTheme.typography.titleMedium
        )

        if (uiState.isApiTokenConfigured) {
            Text(
                text = "Token API configuré et chiffré sur cet appareil",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(
                onClick = onClearApiToken,
                enabled = !uiState.isSavingToken
            ) {
                Text("Remplacer le token")
            }
        } else {
            OutlinedTextField(
                value = token,
                onValueChange = {
                    token = it
                    onClearError()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSavingToken,
                label = { Text("Token API Wasender") },
                supportingText = {
                    Text("Le token sera chiffré avec Android Keystore")
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )
            Button(
                onClick = { onSaveApiToken(token) },
                enabled = token.isNotBlank() && !uiState.isSavingToken
            ) {
                if (uiState.isSavingToken) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Enregistrer le token")
                }
            }
        }

        uiState.error?.let { error ->
            Text(
                text = "Erreur : $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    YanguServiceTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(),
            onSaveApiToken = {},
            onClearApiToken = {},
            onClearError = {},
            onBack = {},
        )
    }
}
