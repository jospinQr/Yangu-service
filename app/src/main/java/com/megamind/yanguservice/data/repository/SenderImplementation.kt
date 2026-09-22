package com.megamind.yanguservice.data.repository

import android.util.Log
import com.megamind.yanguservice.data.remote.WasenderSevice
import com.megamind.yanguservice.domain.utils.ImageAttachment
import com.megamind.yanguservice.domain.utils.normalizeRecipientPhoneNumber
import com.megamind.yanguservice.domain.repo.SenderRepository
import com.megamind.yanguservice.utlis.BulkSendResult
import com.megamind.yanguservice.utlis.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class SenderRepositoryImpl(
    private val service: WasenderSevice,
    private val dispatcher: CoroutineDispatcher
) : SenderRepository {

    override suspend fun sendMessage(message: String, phoneNumber: String): Result<String> =
        withContext(dispatcher) {
            val number = normalizeRecipientPhoneNumber(phoneNumber)
                ?: return@withContext IllegalArgumentException(
                    "Numéro invalide : $phoneNumber"
                ).let { error ->
                    logFailure("Validation du destinataire", phoneNumber, error)
                    Result.Error(error)
                }
            service.sendMessage(message, number).also { result ->
                if (result is Result.Error) {
                    logFailure("Envoi du message", number, result.e)
                }
            }
        }

    override suspend fun sendToMany(
        message: String,
        phoneNumbers: List<String>
    ): BulkSendResult = withContext(dispatcher) {
        sendToRecipients(phoneNumbers) { number ->
            service.sendMessage(message, number)
        }
    }

    override suspend fun sendImageToMany(
        caption: String,
        phoneNumbers: List<String>,
        image: ImageAttachment
    ): BulkSendResult = withContext(dispatcher) {
        val recipients = prepareRecipients(phoneNumbers)
        if (recipients.valid.isEmpty()) {
            return@withContext BulkSendResult(emptyList(), recipients.invalid)
        }

        when (val uploadResult = service.uploadImage(image)) {
            is Result.Success -> {
                val imageUrl = uploadResult.data
                if (imageUrl.isNullOrBlank()) {
                    val error = IllegalStateException("L'upload de l'image n'a retourné aucune URL")
                    logFailure("Upload de l'image", null, error)
                    recipients.valid.forEach { recipients.invalid[it] = error }
                    BulkSendResult(emptyList(), recipients.invalid)
                } else {
                    sendToPreparedRecipients(recipients) { number ->
                        service.sendImageMessage(caption, number, imageUrl)
                    }
                }
            }

            is Result.Error -> {
                val error = uploadResult.e ?: IllegalStateException("Échec de l'upload de l'image")
                logFailure("Upload de l'image", null, error)
                recipients.valid.forEach { recipients.invalid[it] = error }
                BulkSendResult(emptyList(), recipients.invalid)
            }
        }
    }

    private suspend fun sendToRecipients(
        phoneNumbers: List<String>,
        send: suspend (String) -> Result<String>
    ): BulkSendResult = sendToPreparedRecipients(prepareRecipients(phoneNumbers), send)

    private suspend fun sendToPreparedRecipients(
        recipients: PreparedRecipients,
        send: suspend (String) -> Result<String>
    ): BulkSendResult {
        val succeeded = mutableListOf<String>()
        val failed = recipients.invalid

        recipients.valid.forEachIndexed { index, number ->
            when (val result = send(number)) {
                is Result.Success -> succeeded.add(number)
                is Result.Error -> {
                    val error = result.e ?: IllegalStateException("Échec de l'envoi")
                    logFailure("Envoi WhatsApp", number, error)
                    failed[number] = error
                }
            }

            if (index < recipients.valid.lastIndex) {
                delay(Random.nextLong(5_000, 10_000).milliseconds)
            }
        }

        return BulkSendResult(succeeded, failed)
    }

    private fun prepareRecipients(phoneNumbers: List<String>): PreparedRecipients {
        val valid = linkedSetOf<String>()
        val invalid = linkedMapOf<String, Throwable>()

        phoneNumbers.forEach { rawNumber ->
            val normalized = normalizeRecipientPhoneNumber(rawNumber)
            if (normalized == null) {
                val error = IllegalArgumentException("Numéro invalide : $rawNumber")
                logFailure("Validation du destinataire", rawNumber, error)
                invalid[rawNumber] = error
            } else {
                valid.add(normalized)
            }
        }

        return PreparedRecipients(valid.toList(), invalid)
    }

    private fun logFailure(operation: String, recipient: String?, error: Throwable?) {
        val actualError = error ?: IllegalStateException("Erreur inconnue")
        val rootCause = generateSequence(actualError) { it.cause }.last()
        val recipientContext = recipient?.let { " pour $it" }.orEmpty()
        val realMessage = rootCause.message
            ?: actualError.message
            ?: rootCause::class.java.simpleName

        Log.e(
            "SenderRepository",
            "$operation$recipientContext échoué : $realMessage",
            actualError
        )
    }

    private data class PreparedRecipients(
        val valid: List<String>,
        val invalid: MutableMap<String, Throwable>
    )
}
