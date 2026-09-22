package com.megamind.yanguservice.data.repository

import com.megamind.yanguservice.data.telephony.SmsGateway
import com.megamind.yanguservice.domain.repo.SmsRepository
import com.megamind.yanguservice.domain.utils.normalizeRecipientPhoneNumber
import com.megamind.yanguservice.utlis.BulkSendResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class SmsRepositoryImpl(
    private val gateway: SmsGateway,
    private val dispatcher: CoroutineDispatcher
) : SmsRepository {
    override suspend fun sendToMany(message: String, phoneNumbers: List<String>): BulkSendResult =
        withContext(dispatcher) {
            val succeeded = mutableListOf<String>()
            val failed = linkedMapOf<String, Throwable>()
            val uniqueNumbers = linkedSetOf<String>()

            phoneNumbers.forEach { rawNumber ->
                val number = normalizeRecipientPhoneNumber(rawNumber)
                if (number == null) {
                    failed[rawNumber] = IllegalArgumentException("Numéro invalide : $rawNumber")
                } else {
                    uniqueNumbers += number
                }
            }

            uniqueNumbers.forEach { number ->
                try {
                    gateway.send(number, message)
                    succeeded += number
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    failed[number] = error
                }
            }

            BulkSendResult(succeeded, failed)
        }
}
