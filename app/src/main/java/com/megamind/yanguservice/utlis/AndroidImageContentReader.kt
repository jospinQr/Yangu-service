package com.megamind.yanguservice.utlis

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.megamind.yanguservice.domain.utils.ImageAttachment
import com.megamind.yanguservice.domain.utils.ImageContentReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AndroidImageContentReader(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher
) : ImageContentReader {

    override suspend fun read(uri: String): Result<ImageAttachment> = withContext(dispatcher) {
        try {
            val contentUri = uri.toUri()
            val mimeType = context.contentResolver.getType(contentUri)
                ?: return@withContext Result.Error(
                    IllegalArgumentException("Impossible de déterminer le type de l'image")
                )

            if (mimeType !in SUPPORTED_MIME_TYPES) {
                return@withContext Result.Error(
                    IllegalArgumentException("Format non pris en charge. Choisissez une image JPEG ou PNG")
                )
            }

            val metadata = readMetadata(contentUri)
            if (metadata.size != null && metadata.size > MAX_IMAGE_BYTES) {
                return@withContext Result.Error(
                    IllegalArgumentException("L'image ne doit pas dépasser 5 Mo")
                )
            }

            val bytes = context.contentResolver.openInputStream(contentUri)?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var totalBytes = 0

                while (true) {
                    val readCount = input.read(buffer)
                    if (readCount == -1) break
                    totalBytes += readCount
                    if (totalBytes > MAX_IMAGE_BYTES) {
                        return@withContext Result.Error(
                            IllegalArgumentException("L'image ne doit pas dépasser 5 Mo")
                        )
                    }
                    output.write(buffer, 0, readCount)
                }
                output.toByteArray()
            } ?: return@withContext Result.Error(
                IllegalArgumentException("Impossible de lire l'image sélectionnée")
            )

            Result.Success(
                ImageAttachment(
                    bytes = bytes,
                    mimeType = mimeType,
                    fileName = metadata.name ?: "image"
                )
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: SecurityException) {
            Result.Error(IllegalStateException("L'accès à l'image a été refusé", exception))
        } catch (exception: Exception) {
            Result.Error(IllegalStateException("Impossible de charger l'image", exception))
        }
    }

    private fun readMetadata(uri: Uri): ImageMetadata {
        var name: String? = null
        var size: Long? = null

        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }

        return ImageMetadata(name = name, size = size)
    }

    private data class ImageMetadata(val name: String?, val size: Long?)

    private companion object {
        const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
        val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/png")
    }
}