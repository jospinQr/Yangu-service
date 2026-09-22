package com.megamind.yanguservice.domain.repo

import com.megamind.yanguservice.utlis.BulkSendResult

interface SmsRepository {
    suspend fun sendToMany(message: String, phoneNumbers: List<String>): BulkSendResult
}
