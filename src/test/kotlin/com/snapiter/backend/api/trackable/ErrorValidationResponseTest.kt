package com.snapiter.backend.api.trackable

import com.snapiter.backend.api.GlobalExceptionHandler
import com.snapiter.backend.model.trackable.trip.TripRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

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

    @Test
    fun `should map 404 error to the standard error response`() {
        whenever(tripRepository.findBySlugAndTrackableId(any(), any())).thenReturn(Mono.empty())

        client.put()
            .uri("/api/trackables/track-123/trips/does-not-exist/active")
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.error").isEqualTo("not_found")
            .jsonPath("$.message").isEqualTo("Trip not found")
            .jsonPath("$.fields").doesNotExist()
    }
}
