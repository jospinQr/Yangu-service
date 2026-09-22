package com.megamind.yanguservice.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.megamind.yanguservice.R
import com.megamind.yanguservice.domain.model.Contact
import com.megamind.yanguservice.domain.model.SendChannel
import com.megamind.yanguservice.ui.component.MessagetextField
import com.megamind.yanguservice.ui.component.SingleChoiceButton
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
    val context = LocalContext.current
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && viewModel.uiState.value.channel == SendChannel.SMS) viewModel.sendToMany()
        else if (!granted) viewModel.onSmsPermissionDenied()
    }

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
        onChannelSelect = viewModel::selectChannel,
        onImageSelected = viewModel::selectImage,
        onRemoveImage = viewModel::removeImage,
        onRemoveContact = viewModel::removeContact,
        onSend = {
            if (uiState.channel == SendChannel.SMS &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            } else {
                viewModel.sendToMany()
            }
        },
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
    onChannelSelect: (SendChannel) -> Unit,
    onImageSelected: (String) -> Unit,
    onRemoveContact: (Contact) -> Unit,
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
                    Row {
                        Text("Envoyer un ")
                        AnimatedContent(targetState = uiState.channel, label = "title") { channel ->
                            Text(
                                text = if (channel == SendChannel.WHATSAPP) "Whatssap"
                                else "Sms",
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {

                        Icon(
                            painter = painterResource(R.drawable.outline_settings_24),
                            contentDescription = "Paramètres",
                            tint = MaterialTheme.colorScheme.primary
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

                SingleChoiceButton(
                    selected = uiState.channel,
                    onSelect = onChannelSelect,
                    enabled = !uiState.isSending,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(uiState.channel == SendChannel.SMS) {
                    Text(
                        "Les SMS sont envoyés depuis votre carte SIM selon votre forfait.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (uiState.channel == SendChannel.WHATSAPP && !uiState.isApiTokenConfigured) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(
                            width = .7.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
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
                                TextButton(
                                    onClick = onOpenContacts,
                                    enabled = !uiState.isSending,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text("Modifier")
                                }
                            }
                            Column(
                                modifier = Modifier.scrollable(
                                    rememberScrollState(),
                                    orientation = Orientation.Vertical
                                )
                            ) {

                                selectedContacts.take(10).forEach { contact ->

                                    ListItem(
                                        modifier = Modifier,
                                        leadingContent = {
                                            Icon(
                                                painter = painterResource(R.drawable.baseline_person_24),
                                                contentDescription = null
                                            )
                                        },
                                        trailingContent = {
                                            IconButton(
                                                onClick = { onRemoveContact(contact) },
                                                enabled = !uiState.isSending
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.outline_close_small_24),
                                                    contentDescription = "Retirer ${contact.name}"
                                                )
                                            }
                                        },
                                        overlineContent = null,
                                        supportingContent = { Text(contact.phoneNumber) },
                                        colors = ListItemDefaults.colors(),
                                        elevation = ListItemDefaults.elevation(ListItemDefaults.Elevation),
                                        content = { Text(contact.name) },
                                        shapes = ListItemDefaults.shapes(shape = MaterialTheme.shapes.medium),
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))

                                }
                            }

                            if (selectedContacts.size > 10) {
                                Text(
                                    "et ${selectedContacts.size - 10} autre(s)",
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
                        Text(
                            "Un numéro international par ligne, ou séparé par une virgule",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    minLines = if (selectedContacts.isEmpty()) 2 else 1,
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.baseline_people_24),
                            contentDescription = null
                        )
                    }
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
        if (uiState.channel == SendChannel.WHATSAPP && uiState.isImageLoading) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Text("Chargement de l’image…")
            }
        } else {
            uiState.selectedImageUri?.takeIf { uiState.channel == SendChannel.WHATSAPP }
                ?.let { imageUri ->
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
                showImageAction = uiState.channel == SendChannel.WHATSAPP,
                onMessageChange = onMessageChange,
                onTrailingAction = onChoseImage,
                onLeadingAction = onOpenContacts
            )

            IconButton(
                onClick = onSendMessage,
                enabled = uiState.canSend()
            ) {
                if (uiState.isSending) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.baseline_send_24),
                        contentDescription = "Envoyer",
                        tint = MaterialTheme.colorScheme.primary
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
            onChannelSelect = {},
            onImageSelected = {},
            onRemoveContact = {},
            onRemoveImage = {},
            onSend = {},
            onCancel = {},
            onOpenContacts = {},
            onOpenSettings = {},
        )
    }
}
