package com.megamind.yanguservice.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.megamind.yanguservice.ui.theme.YanguServiceTheme
import com.megamind.yanguservice.utlis.BulkSendResult
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SendMessageScreen(
    modifier: Modifier = Modifier,
    viewModel: SenderViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SendMessageScreenContent(
        uiState = uiState,
        onSaveApiToken = viewModel::saveApiToken,
        onClearApiToken = viewModel::clearApiToken,
        onImageSelected = viewModel::selectImage,
        onRemoveImage = viewModel::removeImage,
        onSend = viewModel::sendToMany,
        onCancel = viewModel::cancel,
        onClearError = viewModel::clearError,
        modifier = modifier
    )
}

@Composable
fun SendMessageScreenContent(
    uiState: SenderUiState,
    onSaveApiToken: (String) -> Unit,
    onClearApiToken: () -> Unit,
    onImageSelected: (String) -> Unit,
    onRemoveImage: () -> Unit,
    onSend: (message: String, phoneNumbers: List<String>) -> Unit,
    onCancel: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var message by remember { mutableStateOf("") }
    var recipients by remember { mutableStateOf("") }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onImageSelected(it.toString()) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Envoyer un message WhatsApp",
            style = MaterialTheme.typography.headlineSmall
        )

        ApiTokenSection(
            uiState = uiState,
            onSaveApiToken = onSaveApiToken,
            onClearApiToken = onClearApiToken,
            onClearError = onClearError
        )

        OutlinedTextField(
            value = recipients,
            onValueChange = {
                recipients = it
                onClearError()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSending,
            label = { Text("Destinataires") },
            supportingText = {
                Text("Un numéro international par ligne, ou séparé par une virgule")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            minLines = 2
        )

        OutlinedTextField(
            value = message,
            onValueChange = {
                message = it
                onClearError()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSending,
            label = {
                Text(
                    if (uiState.selectedImageName == null) "Message"
                    else "Légende (facultative)"
                )
            },
            minLines = 4
        )

        ImageSelection(
            uiState = uiState,
            onPickImage = {
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemoveImage = onRemoveImage
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onSend(message, recipients.toPhoneNumbers()) },
                enabled = !uiState.isSending &&
                    !uiState.isImageLoading &&
                    uiState.isApiTokenConfigured &&
                    recipients.isNotBlank() &&
                    (message.isNotBlank() || uiState.selectedImageName != null),
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Envoyer")
                }
            }

            if (uiState.isSending) {
                OutlinedButton(onClick = onCancel) {
                    Text("Annuler")
                }
            }
        }

        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        uiState.result?.let { result ->
            SendResult(result)
        }
    }
}

@Composable
private fun ApiTokenSection(
    uiState: SenderUiState,
    onSaveApiToken: (String) -> Unit,
    onClearApiToken: () -> Unit,
    onClearError: () -> Unit
) {
    var token by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isApiTokenConfigured) {
        if (uiState.isApiTokenConfigured) token = ""
    }

    if (uiState.isApiTokenConfigured) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Token API configuré et chiffré sur cet appareil",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(
                onClick = onClearApiToken,
                enabled = !uiState.isSending
            ) {
                Text("Remplacer le token")
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
}

@Composable
private fun ImageSelection(
    uiState: SenderUiState,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit
) {
    when {
        uiState.isImageLoading -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Text("Chargement de l'image…")
            }
        }

        uiState.selectedImageName != null -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.selectedImageUri?.let { imageUri ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = Uri.parse(imageUri),
                            contentDescription = "Aperçu de l'image sélectionnée",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Text(
                    text = "Image : ${uiState.selectedImageName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPickImage, enabled = !uiState.isSending) {
                        Text("Remplacer")
                    }
                    OutlinedButton(onClick = onRemoveImage, enabled = !uiState.isSending) {
                        Text("Retirer")
                    }
                }
            }
        }

        else -> {
            OutlinedButton(onClick = onPickImage, enabled = !uiState.isSending) {
                Text("Choisir une image")
            }
            Text(
                text = "Formats JPEG ou PNG, 5 Mo maximum",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SendResult(result: BulkSendResult) {
    val color = if (result.failed.isNotEmpty()) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "${result.succeeded.size} message(s) envoyé(s)",
            color = color,
            style = MaterialTheme.typography.titleMedium
        )
        if (result.failed.isNotEmpty()) {
            Text(
                text = "${result.failed.size} échec(s) : ${result.failed.keys.joinToString()}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun String.toPhoneNumbers(): List<String> =
    split(Regex("[,;\\n]+"))
        .map(String::trim)
        .filter(String::isNotEmpty)

@Preview(showBackground = true)
@Composable
private fun SendMessageScreenPreview() {
    YanguServiceTheme {
        SendMessageScreenContent(
            uiState = SenderUiState(),
            onSaveApiToken = {},
            onClearApiToken = {},
            onImageSelected = {},
            onRemoveImage = {},
            onSend = { _, _ -> },
            onCancel = {},
            onClearError = {}
        )
    }
}
