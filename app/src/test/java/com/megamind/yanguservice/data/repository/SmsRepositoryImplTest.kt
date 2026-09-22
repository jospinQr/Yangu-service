package com.megamind.yanguservice.data.repository

import com.megamind.yanguservice.data.telephony.SmsGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsRepositoryImplTest {
    @Test
    fun sendsValidUniqueNumbersAndReportsFailures() = runBlocking {
        val sent = mutableListOf<String>()
        val gateway = object : SmsGateway {
            override suspend fun send(phoneNumber: String, message: String) {
                sent += phoneNumber
                if (phoneNumber == "+33712345678") error("Réseau indisponible")
            }
        }
        val repository = SmsRepositoryImpl(gateway, Dispatchers.Unconfined)

        val result = repository.sendToMany(
            "Bonjour",
            listOf("+33612345678", "+33612345678", "invalide", "+33712345678")
        )

        assertEquals(listOf("+33612345678", "+33712345678"), sent)
        assertEquals(listOf("+33612345678"), result.succeeded)
        assertEquals(setOf("invalide", "+33712345678"), result.failed.keys)
        assertTrue(result.failed["+33712345678"]?.message?.contains("Réseau") == true)
    }
}
