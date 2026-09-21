package com.megamind.yanguservice.domain.utils

import com.megamind.yanguservice.domain.model.VcfContacts

interface VcfContactReader {
    suspend fun read(uri: String): VcfContacts
}
