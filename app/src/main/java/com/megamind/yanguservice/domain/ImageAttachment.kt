package com.megamind.yanguservice.domain

data class ImageAttachment(
    val bytes: ByteArray,
    val mimeType: String,
    val fileName: String
)
