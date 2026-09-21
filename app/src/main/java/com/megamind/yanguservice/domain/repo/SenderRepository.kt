package com.megamind.yanguservice.domain.repo

import com.megamind.yanguservice.domain.utils.ImageAttachment
import com.megamind.yanguservice.utlis.BulkSendResult
import com.megamind.yanguservice.utlis.Result

interface SenderRepository {

    suspend fun sendMessage(message: String, phoneNumber: String): Result<String>
    suspend fun sendToMany(message: String, phoneNumbers: List<String>): BulkSendResult
    suspend fun sendImageToMany(
        caption: String,
        phoneNumbers: List<String>,
        image: ImageAttachment
    ): BulkSendResult
}