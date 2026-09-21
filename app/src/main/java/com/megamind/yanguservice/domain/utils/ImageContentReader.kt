package com.megamind.yanguservice.domain.utils

import com.megamind.yanguservice.utlis.Result

interface ImageContentReader {
    suspend fun read(uri: String): Result<ImageAttachment>
}
