package com.megamind.yanguservice.domain.usecase

import com.megamind.yanguservice.domain.model.Contact
import com.megamind.yanguservice.domain.model.ContactImportResult
import com.megamind.yanguservice.domain.repo.ContactRepository
import com.megamind.yanguservice.domain.utils.VcfContactReader
import com.megamind.yanguservice.domain.utils.normalizeInternationalNumber
import java.util.UUID

class ImportVcfContacts(
    private val reader: VcfContactReader,
    private val repository: ContactRepository,
) {
    suspend operator fun invoke(uri: String): ContactImportResult {
        val read = reader.read(uri)
        require(read.cardsRead > 0) { "Ce fichier ne contient aucune vCard" }

        val contacts = read.contacts.mapNotNull { candidate ->
            val name = candidate.name.trim()
            val phoneNumber = normalizeInternationalNumber(candidate.phoneNumber)
            if (name.isEmpty() || phoneNumber == null) null
            else Contact(UUID.randomUUID().toString(), name, phoneNumber)
        }
        val imported = repository.importContacts(contacts)
        return ContactImportResult(
            imported = imported,
            skipped = read.skipped + read.contacts.size - imported,
        )
    }
}
