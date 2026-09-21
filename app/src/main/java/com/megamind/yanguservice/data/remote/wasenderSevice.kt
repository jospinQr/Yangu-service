package com.megamind.yanguservice.data.remote

import com.megamind.yanguservice.data.remote.utils.safeApiCall
import com.megamind.yanguservice.domain.utils.ImageAttachment
import com.megamind.yanguservice.utlis.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class WasenderSevice(private val httpClient: HttpClient) {

    suspend fun sendMessage(message: String, phoneNumber: String): Result<String> =
        safeApiCall {
            httpClient.post("send-message") {
                setBody(SendMessageRequest(to = phoneNumber, text = message))
            }
        }

    suspend fun uploadImage(image: ImageAttachment): Result<String> {
        return when (
            val result = safeApiCall<UploadMediaResponse> {
                httpClient.post("upload") {
                    contentType(ContentType.parse(image.mimeType))
                    setBody(image.bytes)
                }
            }
        ) {
            is Result.Success -> {
                val publicUrl = result.data?.publicUrl
                if (publicUrl.isNullOrBlank()) {
                    Result.Error(IllegalStateException("L'API n'a retourné aucune URL pour l'image"))
                } else {
                    Result.Success(publicUrl)
                }
            }

            is Result.Error -> Result.Error(
                result.e ?: IllegalStateException("Échec de l'upload de l'image")
            )
        }
    }

    suspend fun sendImageMessage(
        caption: String,
        phoneNumber: String,
        imageUrl: String
    ): Result<String> = safeApiCall {
        httpClient.post("send-message") {
            setBody(
                SendImageMessageRequest(
                    to = phoneNumber,
                    text = caption.ifBlank { null },
                    imageUrl = imageUrl
                )
            )
        }
    }
}

@Serializable
data class SendMessageRequest(val to: String, val text: String)

@Serializable
data class SendImageMessageRequest(
    val to: String,
    val text: String? = null,
    val imageUrl: String
)

@Serializable
private data class UploadMediaResponse(
    val success: Boolean,
    val publicUrl: String? = null
)
