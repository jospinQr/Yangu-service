package com.megamind.yanguservice.domain.usecase

import com.megamind.yanguservice.domain.model.SendChannel
import com.megamind.yanguservice.domain.repo.SenderRepository
import com.megamind.yanguservice.domain.repo.SmsRepository
import com.megamind.yanguservice.domain.utils.ImageAttachment
import com.megamind.yanguservice.utlis.BulkSendResult
import com.megamind.yanguservice.utlis.Result
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SendMessagesTest {
    @Test
    fun routesSmsAndWhatsappSeparately() = runBlocking {
        val calls = mutableListOf<String>()
        val whatsApp = object : SenderRepository {
            override suspend fun sendMessage(message: String, phoneNumber: String): Result<String> =
                error("Unexpected single send")

            override suspend fun sendToMany(message: String, phoneNumbers: List<String>): BulkSendResult {
                calls += "whatsapp-text"
                return BulkSendResult(phoneNumbers, emptyMap())
            }

            override suspend fun sendImageToMany(
                caption: String,
                phoneNumbers: List<String>,
                image: ImageAttachment
            ): BulkSendResult {
                calls += "whatsapp-image"
                return BulkSendResult(phoneNumbers, emptyMap())
            }
        }
        val sms = object : SmsRepository {
            override suspend fun sendToMany(message: String, phoneNumbers: List<String>): BulkSendResult {
                calls += "sms"
                return BulkSendResult(phoneNumbers, emptyMap())
            }
        }
        val sendMessages = SendMessages(whatsApp, sms)
        val numbers = listOf("+33612345678")
        val image = ImageAttachment(byteArrayOf(1), "image/png", "photo.png")

        sendMessages(SendChannel.SMS, "Bonjour", numbers, image)
        sendMessages(SendChannel.WHATSAPP, "Bonjour", numbers, null)
        sendMessages(SendChannel.WHATSAPP, "Bonjour", numbers, image)

        assertEquals(listOf("sms", "whatsapp-text", "whatsapp-image"), calls)
    }
}
