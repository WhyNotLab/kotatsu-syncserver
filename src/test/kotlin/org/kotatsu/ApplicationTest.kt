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
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import org.kotatsu.model.favourite.FavouritesPackage
import org.kotatsu.model.history.HistoryPackage
import org.kotatsu.plugins.configureLogging
import org.kotatsu.plugins.configureSerialization
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

        @Test
        fun testFavouritesPayloadIgnoresUnknownFields() = testApplication {
                application {
                        configureSerialization()
                        routing {
                                post<FavouritesPackage>("/resource/favourites") {
                                        call.respondText("OK", status = HttpStatusCode.OK)
                                }
                        }
                }

                val payload = """
                        {
                            "categories": [
                                {
                                    "category_id": 1,
                                    "created_at": 1,
                                    "sort_key": 0,
                                    "title": "Read later",
                                    "order": "NEWEST",
                                    "track": true,
                                    "show_in_lib": true,
                                    "deleted_at": 0,
                                    "unexpected_category_field": "ignored"
                                }
                            ],
                            "favourites": [
                                {
                                    "manga_id": 10,
                                    "category_id": 1,
                                    "sort_key": 0,
                                    "pinned": false,
                                    "created_at": 2,
                                    "deleted_at": 0,
                                    "manga": {
                                        "manga_id": 10,
                                        "title": "Title",
                                        "alt_title": null,
                                        "url": "/title",
                                        "public_url": "https://example.com/title",
                                        "rating": 0.5,
                                        "content_rating": null,
                                        "cover_url": "https://example.com/cover.jpg",
                                        "large_cover_url": null,
                                        "tags": [
                                            {
                                                "tag_id": 100,
                                                "title": "Action",
                                                "key": "action",
                                                "source": "TEST",
                                                "unexpected_tag_field": true
                                            }
                                        ],
                                        "state": null,
                                        "author": null,
                                        "source": "TEST",
                                        "unexpected_manga_field": "ignored"
                                    }
                                }
                            ],
                            "timestamp": 3,
                            "unexpected_root_field": "ignored"
                        }
                """.trimIndent()

                client.post("/resource/favourites") {
                        contentType(ContentType.Application.Json)
                        setBody(payload)
                }.apply {
                        assertEquals(HttpStatusCode.OK, status)
                        assertEquals("OK", bodyAsText())
                }
        }

        @Test
        fun testHistoryPayloadIgnoresUnknownFields() = testApplication {
                application {
                        configureSerialization()
                        routing {
                                post<HistoryPackage>("/resource/history") {
                                        call.respondText("OK", status = HttpStatusCode.OK)
                                }
                        }
                }

                val payload = """
                        {
                            "history": [
                                {
                                    "manga_id": 10,
                                    "created_at": 1,
                                    "updated_at": 2,
                                    "chapter_id": 3,
                                    "page": 4,
                                    "scroll": 0.25,
                                    "percent": 0.5,
                                    "chapters": 12,
                                    "deleted_at": 0,
                                    "manga": {
                                        "manga_id": 10,
                                        "title": "Title",
                                        "alt_title": null,
                                        "url": "/title",
                                        "public_url": "https://example.com/title",
                                        "rating": 0.5,
                                        "content_rating": null,
                                        "cover_url": "https://example.com/cover.jpg",
                                        "large_cover_url": null,
                                        "tags": [],
                                        "state": null,
                                        "author": null,
                                        "source": "TEST",
                                        "unexpected_manga_field": "ignored"
                                    },
                                    "unexpected_history_field": "ignored"
                                }
                            ],
                            "timestamp": 3,
                            "unexpected_root_field": "ignored"
                        }
                """.trimIndent()

                client.post("/resource/history") {
                        contentType(ContentType.Application.Json)
                        setBody(payload)
                }.apply {
                        assertEquals(HttpStatusCode.OK, status)
                        assertEquals("OK", bodyAsText())
                }
        }
}

private class TestAppender : AppenderBase<ILoggingEvent>() {
    val events = mutableListOf<ILoggingEvent>()

    override fun append(eventObject: ILoggingEvent) {
        events += eventObject
    }
}
