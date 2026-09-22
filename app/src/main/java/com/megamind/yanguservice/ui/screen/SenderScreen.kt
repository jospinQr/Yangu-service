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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import coil3.compose.AsyncImage
import com.megamind.yanguservice.R
import com.megamind.yanguservice.ui.component.MessagetextField
import com.megamind.yanguservice.ui.theme.YanguServiceTheme
import com.megamind.yanguservice.utlis.BulkSendResult
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SendMessageScreen(
    modifier: Modifier = Modifier,
    onOpenContacts: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
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
        onMessageChange = viewModel::updateMessage,
        onRecipientsChange = viewModel::updateRecipientsInput,
        onImageSelected = viewModel::selectImage,
        onRemoveImage = viewModel::removeImage,
        onSend = viewModel::sendToMany,
        onCancel = viewModel::cancel,
        onOpenContacts = onOpenContacts,
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )
}

@Composable
fun SendMessageScreenContent(
    uiState: SenderUiState,
    onMessageChange: (String) -> Unit,
    onRecipientsChange: (String) -> Unit,
    onImageSelected: (String) -> Unit,
    onRemoveImage: () -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedContacts = uiState.selectedContacts
    val phoneNumbers = uiState.phoneNumbers
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onImageSelected(it.toString()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedContacts.isEmpty()) "Envoyer un message WhatsApp"
                        else "Préparer le message",
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {

                        Icon(
                            painter = painterResource(R.drawable.outline_settings_24),
                            contentDescription = "Paramètres"
                        )
                    }
                },

                )

        },

        bottomBar = {
            BottomBarContent(
                onMessageChange = onMessageChange,
                uiState = uiState,
                onRemoveImage = onRemoveImage,
                onSendMessage = onSend,
                onChoseImage = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onOpenContacts = onOpenContacts
            )
        }

    ) { innerPqdding ->

        Box(modifier = modifier.padding(innerPqdding)) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            )
            {

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

                if (selectedContacts.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${selectedContacts.size} contact(s) sélectionné(s)",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                TextButton(onClick = onOpenContacts, enabled = !uiState.isSending) {
                                    Text("Modifier")
                                }
                            }
                            selectedContacts.take(3).forEach { contact ->
                                Text(
                                    "${contact.name} · ${contact.phoneNumber}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (selectedContacts.size > 3) {
                                Text(
                                    "et ${selectedContacts.size - 3} autre(s)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.recipientsInput,
                    onValueChange = onRecipientsChange,
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


                if (phoneNumbers.size > 1 && !uiState.isSending) {
                    Text(
                        "${phoneNumbers.size} destinataires · envoi successif",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                uiState.error?.let { error ->
                    Text(
                        text = "Erreur : $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                uiState.result?.let { result ->
                    SendResult(result)
                }
            }
        }
    }

}

@Composable
fun BottomBarContent(
    onMessageChange: (String) -> Unit,
    uiState: SenderUiState,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier,
    onSendMessage: () -> Unit = {},
    onChoseImage: () -> Unit = {},
    onOpenContacts: () -> Unit = {},

    ) {


    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (uiState.isImageLoading) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Text("Chargement de l’image…")
            }
        } else {
            uiState.selectedImageUri?.let { imageUri ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = Uri.parse(imageUri),
                            contentDescription = "Aperçu de l’image sélectionnée",
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = uiState.selectedImageName ?: "Image sélectionnée",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2
                        )
                        TextButton(onClick = onRemoveImage, enabled = !uiState.isSending) {
                            Text("Retirer")
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            MessagetextField(
                modifier = Modifier.weight(1f),
                message = uiState.message,
                enabled = !uiState.isSending,
                onMessageChange = onMessageChange,
                onTrailingAction = onChoseImage,
                onLeadingAction = onOpenContacts
            )

            IconButton(
                onClick = onSendMessage,
                enabled = uiState.canSend()
            ) {
                if (uiState.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.baseline_send_24),
                        contentDescription = "Envoyer"
                    )
                }
            }
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

@Preview(showBackground = true)
@Composable
private fun SendMessageScreenPreview() {
    YanguServiceTheme {
        SendMessageScreenContent(
            uiState = SenderUiState(),
            onMessageChange = {},
            onRecipientsChange = {},
            onImageSelected = {},
            onRemoveImage = {},
            onSend = {},
            onCancel = {},
            onOpenContacts = {},
            onOpenSettings = {},
        )
    }
}
