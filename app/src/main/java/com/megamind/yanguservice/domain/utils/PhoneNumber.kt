package com.megamind.yanguservice.domain.utils

private val INTERNATIONAL_NUMBER = Regex("^\\+[1-9]\\d{7,14}$")

fun normalizeInternationalNumber(raw: String): String? {
    val number = if (raw.startsWith("tel:", ignoreCase = true)) {
        raw.substring(4).substringBefore(';')
    } else {
        raw
    }
    val cleaned = number.filterNot {
        it.isWhitespace() || it == '-' || it == '.' || it == '(' || it == ')'
    }
    return cleaned.takeIf(INTERNATIONAL_NUMBER::matches)
}
