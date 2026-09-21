package com.megamind.yanguservice.data.local.vcf

import com.megamind.yanguservice.domain.model.ContactDraft
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class VcfParserTest {
    @Test
    fun readsFoldedNameMultipleNumbersAndStructuredName() {
        val vcf = listOf(
            "BEGIN:VCARD",
            "VERSION:3.0",
            "FN:Marie ",
            " Dupont",
            "TEL;TYPE=CELL:+33 6 12 34 56 78",
            "TEL;TYPE=WORK:+33 7 12 34 56 78",
            "END:VCARD",
            "BEGIN:VCARD",
            "VERSION:3.0",
            "N:Martin;Alice;;;",
            "TEL;VALUE=uri:tel:+33698765432",
            "END:VCARD",
            "BEGIN:VCARD",
            "VERSION:3.0",
            "FN:Sans téléphone",
            "END:VCARD",
        ).joinToString("\r\n", postfix = "\r\n")

        val result = VcfParser().parse(ByteArrayInputStream(vcf.toByteArray(Charsets.UTF_8)))

        assertEquals(3, result.cardsRead)
        assertEquals(1, result.skipped)
        assertEquals(
            listOf(
                ContactDraft("Marie Dupont", "+33 6 12 34 56 78"),
                ContactDraft("Marie Dupont", "+33 7 12 34 56 78"),
                ContactDraft("Alice Martin", "+33698765432"),
            ),
            result.contacts,
        )
    }
}
