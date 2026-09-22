package com.megamind.yanguservice.domain.usecase

import com.megamind.yanguservice.domain.model.SendChannel
import com.megamind.yanguservice.domain.repo.SenderRepository
import com.megamind.yanguservice.domain.repo.SmsRepository
import com.megamind.yanguservice.domain.utils.ImageAttachment
import com.megamind.yanguservice.utlis.BulkSendResult

class SendMessages(
    private val whatsAppRepository: SenderRepository,
    private val smsRepository: SmsRepository
) {
    suspend operator fun invoke(
        channel: SendChannel,
        message: String,
        phoneNumbers: List<String>,
        image: ImageAttachment?
    ): BulkSendResult = when (channel) {
        SendChannel.WHATSAPP -> image?.let {
            whatsAppRepository.sendImageToMany(message, phoneNumbers, it)
        } ?: whatsAppRepository.sendToMany(message, phoneNumbers)
        SendChannel.SMS -> smsRepository.sendToMany(message, phoneNumbers)
    }
}
