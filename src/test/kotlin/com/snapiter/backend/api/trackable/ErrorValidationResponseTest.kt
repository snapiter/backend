package com.snapiter.backend.api.trackable

import com.snapiter.backend.api.GlobalExceptionHandler
import com.snapiter.backend.model.trackable.trip.TripRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

class ErrorValidationResponseTest {
    private val tripRepository: TripRepository = mock()
    private val client: WebTestClient =
        WebTestClient.bindToController(TripController(tripRepository))
            .controllerAdvice(GlobalExceptionHandler())
            .build()

    @Test
    fun `should fail on empty title and on incorrect slug`() {
        val json = """
            {
              "title": "",
              "slug": "Not A Slug",
              "startDate": "2025-01-01T10:00:00Z",
              "endDate": "2025-01-05T10:00:00Z",
              "positionType": "HOURLY",
              "color": "648192",
              "animationSpeed": 10000
            }
        """.trimIndent()

        client.post()
            .uri("/api/trackables/track-123/trips")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(json)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.error").isEqualTo("validation_error")
            .jsonPath("$.message").isEqualTo("One or more fields are invalid")
            .jsonPath("$.fields.title").exists()
            .jsonPath("$.fields.slug").exists()
    }

    @Test
    fun `should fail on broken json value`() {
        // invalid JSON syntax
        val json = """{ "title": "Broken", "slug": }"""

        client.post()
            .uri("/api/trackables/track-123/trips")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(json)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.error").isEqualTo("malformed_request")
            .jsonPath("$.fields").doesNotExist()
    }
}
