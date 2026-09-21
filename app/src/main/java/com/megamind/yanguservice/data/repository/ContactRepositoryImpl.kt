package com.megamind.yanguservice.data.repository

import androidx.room.withTransaction
import com.megamind.yanguservice.data.local.AppDataBase
import com.megamind.yanguservice.data.local.dao.ContactDao
import com.megamind.yanguservice.data.local.toDomain
import com.megamind.yanguservice.data.local.toEntity
import com.megamind.yanguservice.domain.model.Contact
import com.megamind.yanguservice.domain.repo.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactRepositoryImpl(
    private val dao: ContactDao,
    private val database: AppDataBase,
) : ContactRepository {
    override fun observeContacts(): Flow<List<Contact>> =
        dao.observeAll().map { contacts -> contacts.map { it.toDomain() } }

    override suspend fun getContact(id: String): Contact? = dao.getById(id)?.toDomain()

    override suspend fun saveContact(contact: Contact) = dao.upsert(contact.toEntity())

    override suspend fun importContacts(contacts: List<Contact>): Int = database.withTransaction {
        val existingNumbers = dao.getAllPhoneNumbers().toHashSet()
        val newContacts = contacts.filter { existingNumbers.add(it.phoneNumber) }
        if (newContacts.isNotEmpty()) dao.insertAll(newContacts.map { it.toEntity() })
        newContacts.size
    }

    override suspend fun deleteContact(id: String) = dao.deleteById(id)
}
