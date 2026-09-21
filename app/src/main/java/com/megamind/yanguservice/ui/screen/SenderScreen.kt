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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import coil3.compose.AsyncImage
import com.megamind.yanguservice.domain.model.Contact
import com.megamind.yanguservice.ui.theme.YanguServiceTheme
import com.megamind.yanguservice.utlis.BulkSendResult
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SendMessageScreen(
    modifier: Modifier = Modifier,
    onOpenContacts: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    selectedContacts: List<Contact> = emptyList(),
    viewModel: SenderViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.refreshApiTokenConfiguration()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshApiTokenConfiguration()
    }

    SendMessageScreenContent(
        uiState = uiState,
        onImageSelected = viewModel::selectImage,
        onRemoveImage = viewModel::removeImage,
        onSend = viewModel::sendToMany,
        onCancel = viewModel::cancel,
        onClearError = viewModel::clearError,
        onOpenContacts = onOpenContacts,
        onOpenSettings = onOpenSettings,
        selectedContacts = selectedContacts,
        modifier = modifier
    )
}

@Composable
fun SendMessageScreenContent(
    uiState: SenderUiState,
    onImageSelected: (String) -> Unit,
    onRemoveImage: () -> Unit,
    onSend: (message: String, phoneNumbers: List<String>) -> Unit,
    onCancel: () -> Unit,
    onClearError: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSettings: () -> Unit,
    selectedContacts: List<Contact> = emptyList(),
    modifier: Modifier = Modifier
) {
    var message by rememberSaveable { mutableStateOf("") }
    var recipients by rememberSaveable { mutableStateOf("") }
    val phoneNumbers = (selectedContacts.map(Contact::phoneNumber) + recipients.toPhoneNumbers())
        .distinct()
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (selectedContacts.isEmpty()) "Envoyer un message WhatsApp"
                else "Préparer le message",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
            )
            TextButton(onClick = onOpenSettings, enabled = !uiState.isSending) {
                Text("Paramètres")
            }
        }

        if (selectedContacts.isEmpty()) {
            OutlinedButton(onClick = onOpenContacts, enabled = !uiState.isSending) {
                Text("Choisir des contacts")
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${selectedContacts.size} contact(s) sélectionné(s)",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextButton(onClick = onOpenContacts, enabled = !uiState.isSending) {
                            Text("Modifier")
                        }
                    }
                    selectedContacts.take(3).forEach { contact ->
                        Text(
                            "${contact.name} · ${contact.phoneNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (selectedContacts.size > 3) {
                        Text(
                            "et ${selectedContacts.size - 3} autre(s)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        if (!uiState.isApiTokenConfigured) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Configurez le token API pour pouvoir envoyer des messages")
                    TextButton(onClick = onOpenSettings) { Text("Ouvrir les paramètres") }
                }
            }
        }

        OutlinedTextField(
            value = recipients,
            onValueChange = {
                recipients = it
                onClearError()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSending,
            label = {
                Text(if (selectedContacts.isEmpty()) "Destinataires" else "Numéros supplémentaires")
            },
            supportingText = {
                Text("Un numéro international par ligne, ou séparé par une virgule")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            minLines = if (selectedContacts.isEmpty()) 2 else 1
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
                onClick = { onSend(message, phoneNumbers) },
                enabled = !uiState.isSending &&
                    !uiState.isImageLoading &&
                    uiState.isApiTokenConfigured &&
                    phoneNumbers.isNotEmpty() &&
                    (message.isNotBlank() || uiState.selectedImageName != null),
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isSending) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Text("Envoi en cours")
                    }
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

        if (phoneNumbers.size > 1 && !uiState.isSending) {
            Text(
                "${phoneNumbers.size} destinataires · envoi successif",
                style = MaterialTheme.typography.bodySmall,
            )
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
            onImageSelected = {},
            onRemoveImage = {},
            onSend = { _, _ -> },
            onCancel = {},
            onClearError = {},
            onOpenContacts = {},
            onOpenSettings = {},
        )
    }
}
