package org.kotatsu.plugins

import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            Json {
                coerceInputValues = true
                encodeDefaults = true
                explicitNulls = false
                ignoreUnknownKeys = true
            }
        )
    }
}
