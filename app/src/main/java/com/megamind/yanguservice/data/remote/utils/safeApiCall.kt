package com.megamind.yanguservice.data.remote.utils

import android.util.Log
import com.megamind.yanguservice.utlis.Result
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.net.UnknownHostException

suspend inline fun <reified T> safeApiCall(
    crossinline block: suspend () -> HttpResponse
): Result<T> {
    return try {
        val response = block()

        if (response.status.isSuccess()) {
            Log.d("WasenderApi", "HTTP ${response.status.value} ${response.call.request.url}")
            Result.Success(response.body<T>())
        } else {
            val errorBody = response.bodyAsText()
            val apiMessage = extractMessageFromErrorBody(errorBody)
                ?: response.status.description
            val exception = ApiException(
                code = response.status.value,
                responseMessage = apiMessage,
                errorBody = errorBody
            )

            Log.e(
                "WasenderApi",
                "Échec HTTP ${response.status.value} sur ${response.call.request.url}: " +
                    "$apiMessage | réponse=$errorBody",
                exception
            )
            Result.Error(exception)
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: IOException) {
        Log.e(
            "WasenderApi",
            "Erreur réseau réelle: ${exception.message ?: exception::class.java.simpleName}",
            exception
        )
        val message = if (exception is UnknownHostException) {
            "Impossible de résoudre l'adresse du serveur WasenderAPI. Vérifiez la connexion Internet et le DNS de cet appareil."
        } else {
            "Vérifiez votre connexion au serveur"
        }
        Result.Error(NetworkException(message, exception))
    } catch (exception: Exception) {
        Log.e(
            "WasenderApi",
            "Erreur inattendue réelle: ${exception.message ?: exception::class.java.simpleName}",
            exception
        )
        Result.Error(exception)
    }
}

fun extractMessageFromErrorBody(body: String): String? {
    return try {
        val json = Json.parseToJsonElement(body).jsonObject
        findMessage(json)
    } catch (_: Exception) {
        null
    }
}

private fun findMessage(json: JsonObject): String? {
    val directMessage = listOf("message", "error", "detail")
        .firstNotNullOfOrNull { key ->
            when (val value = json[key]) {
                is JsonPrimitive -> value.contentOrNull
                is JsonObject -> findMessage(value)
                else -> null
            }
        }

    return directMessage ?: (json["data"] as? JsonObject)?.let(::findMessage)
}

class NetworkException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)

class ApiException(
    val code: Int,
    val responseMessage: String,
    val errorBody: String? = null
) : Exception("HTTP $code: $responseMessage")
