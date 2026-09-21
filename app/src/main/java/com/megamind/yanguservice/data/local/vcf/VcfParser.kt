package com.megamind.yanguservice.data.local.vcf

import com.megamind.yanguservice.domain.model.ContactDraft
import com.megamind.yanguservice.domain.model.VcfContacts
import ezvcard.VCard
import ezvcard.io.text.VCardReader
import java.io.InputStream

class VcfParser {
    fun parse(input: InputStream): VcfContacts {
        val contacts = mutableListOf<ContactDraft>()
        var skipped = 0
        var cardsRead = 0

        VCardReader(input).use { reader ->
            while (true) {
                val card = reader.readNext() ?: break
                cardsRead++
                val name = card.contactName()
                if (name.isBlank() || card.telephoneNumbers.isEmpty()) {
                    skipped++
                    continue
                }

                card.telephoneNumbers.forEach { telephone ->
                    val number = (telephone.uri?.number ?: telephone.text)?.trim().orEmpty()
                    if (number.isBlank()) skipped++
                    else contacts += ContactDraft(name, number)
                }
            }
        }

        return VcfContacts(contacts, skipped, cardsRead)
    }

    private fun VCard.contactName(): String {
        val formatted = formattedName?.value?.trim().orEmpty()
        if (formatted.isNotEmpty()) return formatted

        val structured = structuredName ?: return ""
        return buildList {
            addAll(structured.prefixes)
            structured.given?.let(::add)
            addAll(structured.additionalNames)
            structured.family?.let(::add)
            addAll(structured.suffixes)
        }.filter(String::isNotBlank).joinToString(" ").trim()
    }
}
