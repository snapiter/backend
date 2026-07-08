package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.trip.PositionType
import com.snapiter.backend.model.trackable.trip.Trip
import com.snapiter.backend.model.trackable.trip.TripRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

/**
 * Exercises the real JSON decoding path (Spring's Jackson codec + @Valid) for the
 * create-trip endpoint, unlike the unit tests that build a CreateTripRequest directly
 * and therefore bypass Jackson entirely.
 *
 * These tests only pass when jackson-module-kotlin is on the classpath: without it,
 * Jackson ignores Kotlin default parameter values and passes null into the non-null
 * constructor params, which fails deserialization and returns 400.
 */
class CreateTripJsonTest {
    private val tripRepository: TripRepository = mock()
    private val client: WebTestClient =
        WebTestClient.bindToController(TripController(tripRepository)).build()

    @Test
    fun `creating a trip with only the required fields applies Kotlin defaults`() {
        whenever(tripRepository.findBySlugAndTrackableId(eq("my-trip"), eq("track-123")))
            .thenReturn(Mono.empty())
        val saved = argumentCaptor<Trip>()
        whenever(tripRepository.save(saved.capture()))
            .thenAnswer { Mono.just(saved.firstValue) }

        // description, endDate, positionType, color and animationSpeed are all omitted
        val json = """
            {
              "title": "My Trip",
              "slug": "my-trip",
              "startDate": "2025-01-01T10:00:00Z"
            }
        """.trimIndent()

        client.post()
            .uri("/api/trackables/track-123/trips")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(json)
            .exchange()
            .expectStatus().isNoContent

        val trip = saved.firstValue
        assertThat(trip.title).isEqualTo("My Trip")
        assertThat(trip.slug).isEqualTo("my-trip")
        // defaults from CreateTripRequest must have been applied during deserialization
        assertThat(trip.positionType).isEqualTo(PositionType.HOURLY)
        assertThat(trip.color).isEqualTo("#648192")
        assertThat(trip.animationSpeed).isEqualTo(10_000L)
        assertThat(trip.description).isNull()
        assertThat(trip.endDate).isNull()
    }

    @Test
    fun `creating a trip with all fields uses the supplied values`() {
        whenever(tripRepository.findBySlugAndTrackableId(eq("full-trip"), eq("track-123")))
            .thenReturn(Mono.empty())
        val saved = argumentCaptor<Trip>()
        whenever(tripRepository.save(saved.capture()))
            .thenAnswer { Mono.just(saved.firstValue) }

        val json = """
            {
              "title": "Full Trip",
              "description": "With everything set",
              "slug": "full-trip",
              "startDate": "2025-01-01T10:00:00Z",
              "endDate": "2025-01-05T10:00:00Z",
              "positionType": "ALL",
              "color": "abcdef",
              "animationSpeed": 2500
            }
        """.trimIndent()

        client.post()
            .uri("/api/trackables/track-123/trips")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(json)
            .exchange()
            .expectStatus().isNoContent

        val trip = saved.firstValue
        assertThat(trip.description).isEqualTo("With everything set")
        assertThat(trip.positionType).isEqualTo(PositionType.ALL)
        assertThat(trip.color).isEqualTo("#abcdef") // "#" gets prefixed by the controller
        assertThat(trip.animationSpeed).isEqualTo(2500L)
    }

    @Test
    fun `creating a trip without a title fails validation with 400`() {
        val json = """
            {
              "slug": "no-title",
              "startDate": "2025-01-01T10:00:00Z"
            }
        """.trimIndent()

        client.post()
            .uri("/api/trackables/track-123/trips")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(json)
            .exchange()
            .expectStatus().isBadRequest
    }
}
