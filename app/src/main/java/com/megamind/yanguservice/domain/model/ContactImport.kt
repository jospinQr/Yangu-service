package com.megamind.yanguservice.domain.model

data class ContactDraft(val name: String, val phoneNumber: String)

data class VcfContacts(
    val contacts: List<ContactDraft>,
    val skipped: Int,
    val cardsRead: Int,
)

data class ContactImportResult(val imported: Int, val skipped: Int)
