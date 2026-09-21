package com.megamind.yanguservice.ui.screen.contacts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.megamind.yanguservice.domain.model.Contact
import com.megamind.yanguservice.ui.theme.YanguServiceTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactScreen(
    onBack: () -> Unit,
    onSendSelected: (List<Contact>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val vcfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importVcf(it.toString()) }
    }

    ContactScreenContent(
        uiState = uiState,
        onBack = onBack,
        onOpenAddSheet = viewModel::openAddSheet,
        onDismissAddSheet = viewModel::dismissAddSheet,
        onAddContact = viewModel::addContact,
        onPickVcf = {
            vcfPicker.launch(
                arrayOf(
                    "text/vcard",
                    "text/x-vcard",
                    "text/directory",
                    "application/vcard",
                    "application/x-vcard",
                    "text/plain",
                    "application/octet-stream",
                )
            )
        },
        onToggleContact = viewModel::toggleContactSelection,
        onToggleSelectAll = viewModel::toggleSelectAll,
        onSendSelected = {
            val contacts = viewModel.selectedContacts()
            if (contacts.isNotEmpty()) onSendSelected(contacts)
        },
        onClearError = viewModel::clearError,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreenContent(
    uiState: ContactsUiState,
    onBack: () -> Unit,
    onOpenAddSheet: () -> Unit,
    onDismissAddSheet: () -> Unit,
    onAddContact: (name: String, phoneNumber: String) -> Unit,
    onPickVcf: () -> Unit,
    onToggleContact: (String) -> Unit,
    onToggleSelectAll: () -> Unit,
    onSendSelected: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = uiState.contacts.count { it.id in uiState.selectedContactIds }
    val allSelected = uiState.contacts.isNotEmpty() && selectedCount == uiState.contacts.size

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("Retour")
        }

        Text("Contacts", style = MaterialTheme.typography.headlineSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onPickVcf, enabled = !uiState.isImporting) {
                if (uiState.isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Importer .vcf")
                }
            }
            Button(onClick = onOpenAddSheet, enabled = !uiState.isImporting) {
                Text("Ajouter")
            }
        }

        uiState.importResult?.let { result ->
            Text(
                "${result.imported} contact(s) importé(s), ${result.skipped} entrée(s) ignorée(s)",
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (!uiState.isAddSheetOpen) {
            uiState.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }

        if (!uiState.isLoading && uiState.contacts.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$selectedCount sur ${uiState.contacts.size} sélectionnés",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onToggleSelectAll) {
                    Text(if (allSelected) "Tout désélectionner" else "Tout sélectionner")
                }
            }
        }

        when {
            uiState.isLoading -> {
                Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.contacts.isEmpty() -> {
                Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Aucun contact enregistré. Ajoutez votre premier contact.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            else -> {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(uiState.contacts, key = Contact::id) { contact ->
                        val selected = contact.id in uiState.selectedContactIds
                        ListItem(
                            checked = selected,
                            onCheckedChange = { onToggleContact(contact.id) },
                            leadingContent = {
                                Checkbox(checked = selected, onCheckedChange = null)
                            },
                            supportingContent = { Text(contact.phoneNumber) },
                        ) { Text(contact.name) }
                        HorizontalDivider()
                    }
                }
            }
        }

        if (!uiState.isLoading && uiState.contacts.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        if (selectedCount == 0) "Sélectionnez des contacts pour leur écrire"
                        else "$selectedCount destinataire(s) prêt(s) pour l'envoi",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = onSendSelected,
                        enabled = selectedCount > 0,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Préparer le message")
                    }
                }
            }
        }
    }

    if (uiState.isAddSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = onDismissAddSheet,
            sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
                confirmValueChange = { value -> !uiState.isSaving || value != SheetValue.Hidden },
            ),
        ) {
            AddContactSheetContent(
                isSaving = uiState.isSaving,
                error = uiState.error,
                onAddContact = onAddContact,
                onDismiss = onDismissAddSheet,
                onClearError = onClearError,
            )
        }
    }
}

@Composable
private fun AddContactSheetContent(
    isSaving: Boolean,
    error: String?,
    onAddContact: (name: String, phoneNumber: String) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Ajouter un contact", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                onClearError()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nom") },
            singleLine = true,
            enabled = !isSaving,
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
                phoneNumber = it
                onClearError()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Numéro de téléphone") },
            supportingText = { Text("Format international, par exemple +33612345678") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            enabled = !isSaving,
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Annuler")
            }
            Button(
                onClick = { onAddContact(name, phoneNumber) },
                enabled = name.isNotBlank() && phoneNumber.isNotBlank() && !isSaving,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enregistrer")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactScreenPreview() {
    YanguServiceTheme {
        ContactScreenContent(
            uiState = ContactsUiState(
                contacts = listOf(Contact("1", "Alice Martin", "+33612345678")),
                isLoading = false,
                selectedContactIds = setOf("1"),
            ),
            onBack = {},
            onOpenAddSheet = {},
            onDismissAddSheet = {},
            onAddContact = { _, _ -> },
            onPickVcf = {},
            onToggleContact = {},
            onToggleSelectAll = {},
            onSendSelected = {},
            onClearError = {},
        )
    }
}
