package com.megamind.yanguservice.domain.usecase

import com.megamind.yanguservice.domain.model.Contact
import com.megamind.yanguservice.domain.model.ContactDraft
import com.megamind.yanguservice.domain.model.VcfContacts
import com.megamind.yanguservice.domain.repo.ContactRepository
import com.megamind.yanguservice.domain.utils.VcfContactReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportVcfContactsTest {
    @Test
    fun importsOnlyNewInternationalNumbersAndCountsSkippedEntries() = runBlocking {
        val read = VcfContacts(
            contacts = listOf(
                ContactDraft(" Marie Dupont ", "+33 6 12 34 56 78"),
                ContactDraft("Marie Dupont", "+33612345678"),
                ContactDraft("Alice Martin", "0612345678"),
                ContactDraft("Bob Martin", "+33712345678"),
            ),
            skipped = 1,
            cardsRead = 4,
        )
        val reader = object : VcfContactReader {
            override suspend fun read(uri: String): VcfContacts = read
        }
        val repository = FakeContactRepository(setOf("+33712345678"))

        val result = ImportVcfContacts(reader, repository)("document-uri")

        assertEquals(1, result.imported)
        assertEquals(4, result.skipped)
        assertEquals("Marie Dupont", repository.inserted.single().name)
        assertEquals("+33612345678", repository.inserted.single().phoneNumber)
    }

    private class FakeContactRepository(existing: Set<String>) : ContactRepository {
        private val numbers = existing.toMutableSet()
        val inserted = mutableListOf<Contact>()

        override fun observeContacts(): Flow<List<Contact>> = flowOf(emptyList())
        override suspend fun getContact(id: String): Contact? = null
        override suspend fun saveContact(contact: Contact) = Unit
        override suspend fun deleteContact(id: String) = Unit

        override suspend fun importContacts(contacts: List<Contact>): Int {
            contacts.filterTo(inserted) { numbers.add(it.phoneNumber) }
            return inserted.size
        }
    }
}
