package com.megamind.yanguservice.domain

import com.megamind.yanguservice.utlis.Result

interface ImageContentReader {
    suspend fun read(uri: String): Result<ImageAttachment>
}
