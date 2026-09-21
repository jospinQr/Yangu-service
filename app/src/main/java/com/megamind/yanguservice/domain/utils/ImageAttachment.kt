package com.megamind.yanguservice.domain.utils

data class ImageAttachment(
    val bytes: ByteArray,
    val mimeType: String,
    val fileName: String
)
