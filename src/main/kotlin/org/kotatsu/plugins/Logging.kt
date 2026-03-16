package org.kotatsu.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.Level
import io.ktor.http.HttpStatusCode

fun Application.configureLogging() {
    val marker = "[kotatsu-syncserver-2026-03-16]"

    install(CallLogging) {
        level = Level.INFO // Временно включаем INFO для диагностики
        filter { true } // Логируем все запросы
        format { call ->
            val status = call.response.status() ?: HttpStatusCode(0, "Unknown")
            val method = call.request.httpMethod.value
            val uri = call.request.uri
            "${marker} ${status.value} ${status.description}: ${method} - ${uri}"
        }
    }

    // Оставляем отдельный ERROR-лог только для реальных ошибок, чтобы не терять алерты.
    intercept(ApplicationCallPipeline.Monitoring) {
        proceed()
        val status = call.response.status() ?: HttpStatusCode(0, "Unknown")
        if (status.value >= 400) {
            val method = call.request.httpMethod.value
            val uri = call.request.uri
            application.log.error("${marker} ${status.value} ${status.description}: ${method} - ${uri}")
        }
    }
}
