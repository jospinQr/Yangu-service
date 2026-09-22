package com.megamind.yanguservice.ui.screen

import com.megamind.yanguservice.domain.model.Contact
import com.megamind.yanguservice.domain.model.SendChannel
import com.megamind.yanguservice.domain.repo.SenderRepository
import com.megamind.yanguservice.domain.repo.SmsRepository
import com.megamind.yanguservice.domain.usecase.SendMessages
import com.megamind.yanguservice.domain.utils.AuthTokenStore
import com.megamind.yanguservice.domain.utils.ImageAttachment
import com.megamind.yanguservice.domain.utils.ImageContentReader
import com.megamind.yanguservice.utlis.BulkSendResult
import com.megamind.yanguservice.utlis.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderViewModelDraftTest {
    @Test
    fun choosingAndRemovingContactsKeepsTheDraft() {
        val viewModel = SenderViewModel(
            sendMessages = SendMessages(object : SenderRepository {
                override suspend fun sendMessage(message: String, phoneNumber: String): Result<String> =
                    error("Unexpected send")

                override suspend fun sendToMany(message: String, phoneNumbers: List<String>): BulkSendResult =
                    error("Unexpected send")

                override suspend fun sendImageToMany(
                    caption: String,
                    phoneNumbers: List<String>,
                    image: ImageAttachment
                ): BulkSendResult = error("Unexpected send")
            }, object : SmsRepository {
                override suspend fun sendToMany(message: String, phoneNumbers: List<String>): BulkSendResult =
                    error("Unexpected SMS send")
            }),
            imageContentReader = object : ImageContentReader {
                override suspend fun read(uri: String): Result<ImageAttachment> =
                    error("Unexpected image read")
            },
            authTokenStore = object : AuthTokenStore {
                override fun getToken(): String = "configured-token"
                override suspend fun saveToken(token: String): Result<Unit> =
                    error("Unexpected token save")

                override suspend fun clearToken() = error("Unexpected token clear")
            }
        )

        viewModel.updateMessage("Bonjour Alice")
        viewModel.updateRecipientsInput("+33600000000")
        val alice = Contact("alice", "Alice", "+33700000000")
        val bob = Contact("bob", "Bob", "+33800000000")
        viewModel.setSelectedContacts(listOf(alice, bob))

        assertEquals("Bonjour Alice", viewModel.uiState.value.message)
        assertEquals("+33600000000", viewModel.uiState.value.recipientsInput)
        assertEquals(listOf("+33700000000", "+33800000000", "+33600000000"), viewModel.uiState.value.phoneNumbers)
        assertTrue(viewModel.uiState.value.canSend())

        viewModel.removeContact(alice)

        assertEquals(listOf(bob), viewModel.uiState.value.selectedContacts)
        assertEquals("Bonjour Alice", viewModel.uiState.value.message)
        assertEquals(listOf("+33800000000", "+33600000000"), viewModel.uiState.value.phoneNumbers)

        viewModel.setSelectedContacts(emptyList())

        assertEquals("Bonjour Alice", viewModel.uiState.value.message)
        assertEquals(listOf("+33600000000"), viewModel.uiState.value.phoneNumbers)
    }

    @Test
    fun smsRequiresTextButNotWhatsAppToken() {
        val withText = SenderUiState(
            channel = SendChannel.SMS,
            message = "Bonjour",
            recipientsInput = "+33612345678"
        )
        assertTrue(withText.canSend())
        assertTrue(!withText.copy(message = "", selectedImageUri = "content://image").canSend())
        assertTrue(!withText.copy(channel = SendChannel.WHATSAPP).canSend())
    }
}
