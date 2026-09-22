package com.megamind.yanguservice.domain.utils

fun normalizeRecipientPhoneNumber(raw: String): String? {
    val cleaned = raw.filter { it.isDigit() || it == '+' }
    return cleaned.takeIf { Regex("^\\+\\d{8,15}$").matches(it) }
}
