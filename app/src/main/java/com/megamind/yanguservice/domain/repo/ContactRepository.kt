package com.megamind.yanguservice.domain.repo

import com.megamind.yanguservice.domain.model.Contact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun observeContacts(): Flow<List<Contact>>
    suspend fun getContact(id: String): Contact?
    suspend fun saveContact(contact: Contact)
    suspend fun importContacts(contacts: List<Contact>): Int
    suspend fun deleteContact(id: String)
}
