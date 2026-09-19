package com.megamind.yanguservice.di

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders

internal fun HttpClientConfig<*>.configureNetworkLogging() {
    install(Logging) {
        logger = Logger.ANDROID
        level = LogLevel.BODY
        sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }
}
