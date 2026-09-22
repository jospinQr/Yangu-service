package com.megamind.yanguservice.data.telephony

interface SmsGateway {
    suspend fun send(phoneNumber: String, message: String)
}
