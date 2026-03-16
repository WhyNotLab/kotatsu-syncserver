package org.kotatsu

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger as LogbackLogger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import org.kotatsu.plugins.configureLogging
import org.slf4j.Logger as Slf4jLogger
import org.slf4j.LoggerFactory
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        environment {
            config = ApplicationConfig("application.conf")
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Alive", bodyAsText())
        }
    }

    @Test
    fun testSuccessfulRequestsAreNotLoggedAsErrors() = testApplication {
        val appender = TestAppender()
        val logger = LoggerFactory.getLogger(Slf4jLogger.ROOT_LOGGER_NAME) as LogbackLogger
        val previousLevel = logger.level
        logger.level = Level.ERROR
        logger.addAppender(appender)
        appender.start()

        try {
            application {
                configureLogging()
                routing {
                    get("/ok") {
                        call.respondText("OK", status = HttpStatusCode.OK)
                    }
                }
            }

            client.get("/ok")

            assertTrue(appender.events.isEmpty())
        } finally {
            logger.detachAppender(appender)
            logger.level = previousLevel
            appender.stop()
        }
    }

    @Test
    fun testFailedRequestsAreLoggedAsErrors() = testApplication {
        val appender = TestAppender()
        val logger = LoggerFactory.getLogger(Slf4jLogger.ROOT_LOGGER_NAME) as LogbackLogger
        val previousLevel = logger.level
        logger.level = Level.ERROR
        logger.addAppender(appender)
        appender.start()

        try {
            application {
                configureLogging()
                routing {
                    get("/fail") {
                        call.respondText("Failure", status = HttpStatusCode.InternalServerError)
                    }
                }
            }

            client.get("/fail")

            assertTrue(appender.events.any { event ->
                event.level == Level.ERROR && event.formattedMessage.contains("500 Internal Server Error")
            })
        } finally {
            logger.detachAppender(appender)
            logger.level = previousLevel
            appender.stop()
        }
    }
}

private class TestAppender : AppenderBase<ILoggingEvent>() {
    val events = mutableListOf<ILoggingEvent>()

    override fun append(eventObject: ILoggingEvent) {
        events += eventObject
    }
}
