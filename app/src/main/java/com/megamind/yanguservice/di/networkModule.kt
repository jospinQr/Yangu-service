package com.megamind.yanguservice.di

import com.megamind.yanguservice.data.remote.WasenderSevice
import com.megamind.yanguservice.domain.AuthTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single<HttpClient> {
        val tokenStore: AuthTokenStore = get()

        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }

            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }

            configureNetworkLogging()

            defaultRequest {
                url("https://www.wasenderapi.com/api/")
                header(HttpHeaders.ContentType, "application/json")
                tokenStore.getToken()?.takeIf(String::isNotBlank)?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
    }

    single { WasenderSevice(get()) }
}
