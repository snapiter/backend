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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.time.Duration

class TripControllerTest {
    private val tripRepository: TripRepository = mock()
    private val controller = TripController(tripRepository)

    val trip = Trip(
        id = 2L,
        trackableId = "trackableId123",
        startDate = Instant.parse("2019-04-12T02:01:00Z"),
        endDate = null,
        title = "Trip Without End",
        description = null,
        slug = "tripSlug",
        positionType = PositionType.HOURLY,
        createdAt = Instant.now(),
        color = "#648192",
        animationSpeed = 10000
    )

    @Test
    fun `should create and return no content when slug not found`() {
        // given
        val request = CreateTripRequest(
            startDate = Instant.parse("2025-01-01T10:00:00Z"),
            endDate = Instant.parse("2025-01-02T10:00:00Z"),
            title = "My Trip",
            description = "Trip description",
            slug = "my-trip",
            positionType = PositionType.ALL,
            color = "123456",
            animationSpeed = 2
        )

        whenever(tripRepository.findBySlugAndTrackableId(eq(request.slug), eq("track-123")))
            .thenReturn(Mono.empty()) // found -> should trigger conflict error
        whenever(tripRepository.save(any()))
            .thenReturn(Mono.just(Trip(id = 2L, trackableId = "track-123", startDate = request.startDate,
                endDate = request.endDate, title = request.title, description = request.description,
                slug = request.slug, positionType = request.positionType, createdAt = Instant.now(),
                color = "#123456", animationSpeed = request.animationSpeed
            )))

        // when
        val result = controller.createTrip("track-123", request)

        // then
        StepVerifier.create(result)
            .consumeNextWith { response ->
                assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
            .verifyComplete()
    }

    @Test
    fun `createTrip should throw conflict when slug already exists`() {
        // given
        val request = CreateTripRequest(
            startDate = Instant.now(),
            endDate = Instant.now().plus(Duration.ofDays(1)),
            title = "Existing trip",
            description = "desc",
            slug = "existing",
            positionType = PositionType.ALL,
            color = "#abcdef",
            animationSpeed = 1
        )

        whenever(tripRepository.findBySlugAndTrackableId(eq(request.slug), eq("track-123")))
            .thenReturn(Mono.just(trip)) // simulate that it should error

        // when
        val result = controller.createTrip("track-123", request)

        // then
        StepVerifier.create(result)
            .expectErrorSatisfies { error ->
                assertThat(error).isInstanceOf(ResponseStatusException::class.java)
                val ex = error as ResponseStatusException
                assertThat(ex.statusCode).isEqualTo(HttpStatus.CONFLICT)
                assertThat(ex.reason).isEqualTo("Slug already exists for this trackable")
            }
            .verify()
    }
    @Test
    fun `updateTrip should update all fields`() {
        val existingTrip = trip.copy(
            id = 42L,
            trackableId = "track-123",
            slug = "old-slug",
            title = "Old title",
            description = "Old description",
            startDate = Instant.parse("2025-01-01T10:00:00Z"),
            endDate = Instant.parse("2025-01-01T10:00:00Z"),
            positionType = PositionType.HOURLY,
            color = "#abcdef",
            animationSpeed = 5000
        )

        val request = UpdateTripRequest(
            title = "New title",
            description = "New description",
            slug = "new-slug",
            startDate = Instant.parse("2025-01-01T10:00:00Z"),
            endDate = Instant.parse("2025-01-01T10:00:00Z"),
            positionType = PositionType.ALL,
            color = "123456", // no # in input → should get prefixed
            animationSpeed = 9999L
        )

        whenever(tripRepository.findBySlugAndTrackableId(eq("42"), eq("track-123")))
            .thenReturn(Mono.just(existingTrip))

        val captor = argumentCaptor<Trip>()
        whenever(tripRepository.save(captor.capture()))
            .thenAnswer { Mono.just(captor.firstValue) }

        val result = controller.updateTrip("track-123", "42", request)

        StepVerifier.create(result)
            .consumeNextWith { response ->
                assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
            .verifyComplete()

        val saved = captor.firstValue
        assertThat(saved.title).isEqualTo("New title")
        assertThat(saved.description).isEqualTo("New description")
        assertThat(saved.slug).isEqualTo("new-slug")
        assertThat(saved.startDate).isEqualTo(Instant.parse("2025-01-01T10:00:00Z"))
        assertThat(saved.endDate).isEqualTo(Instant.parse("2025-01-01T10:00:00Z"))
        assertThat(saved.positionType).isEqualTo(PositionType.ALL)
        assertThat(saved.color).isEqualTo("#123456")
        assertThat(saved.animationSpeed).isEqualTo(9999L)

        // unchanged
        assertThat(saved.id).isEqualTo(existingTrip.id)
        assertThat(saved.trackableId).isEqualTo(existingTrip.trackableId)
        assertThat(saved.createdAt).isEqualTo(existingTrip.createdAt)
    }

    @Test
    fun `endTrip should set endDate to now when the trip has no endDate`() {
        val existingTrip = trip.copy(
            id = 11L,
            trackableId = "track-123",
            slug = "to-end",
            endDate = null
        )

        whenever(tripRepository.findBySlugAndTrackableId(eq("to-end"), eq("track-123")))
            .thenReturn(Mono.just(existingTrip))
        val captor = argumentCaptor<Trip>()
        whenever(tripRepository.save(captor.capture()))
            .thenAnswer { Mono.just(captor.firstValue) }

        val before = Instant.now()
        val result = controller.endTrip("track-123", "to-end")

        StepVerifier.create(result)
            .consumeNextWith { response ->
                assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
            .verifyComplete()

        val saved = captor.firstValue
        assertThat(saved.endDate).isNotNull()
        assertThat(saved.endDate).isBetween(before, Instant.now())
        // only endDate changes
        assertThat(saved).usingRecursiveComparison().ignoringFields("endDate").isEqualTo(existingTrip)
    }

    @Test
    fun `endTrip should keep the original endDate when already ended`() {
        val originalEnd = Instant.parse("2025-01-05T10:00:00Z")
        val existingTrip = trip.copy(
            id = 12L,
            trackableId = "track-123",
            slug = "already-ended",
            endDate = originalEnd
        )

        whenever(tripRepository.findBySlugAndTrackableId(eq("already-ended"), eq("track-123")))
            .thenReturn(Mono.just(existingTrip))
        val captor = argumentCaptor<Trip>()
        whenever(tripRepository.save(captor.capture()))
            .thenAnswer { Mono.just(captor.firstValue) }

        val result = controller.endTrip("track-123", "already-ended")

        StepVerifier.create(result)
            .consumeNextWith { response ->
                assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
            .verifyComplete()

        // idempotent: end time is unchanged
        assertThat(captor.firstValue.endDate).isEqualTo(originalEnd)
    }

    @Test
    fun `endTrip should throw not found when the trip does not exist`() {
        whenever(tripRepository.findBySlugAndTrackableId(eq("missing"), eq("track-123")))
            .thenReturn(Mono.empty())

        val result = controller.endTrip("track-123", "missing")

        StepVerifier.create(result)
            .expectErrorSatisfies { error ->
                assertThat(error).isInstanceOf(ResponseStatusException::class.java)
                val ex = error as ResponseStatusException
                assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
                assertThat(ex.reason).isEqualTo("Trip not found")
            }
            .verify()

        verify(tripRepository, never()).save(any())
    }

    @Test
    fun `deleteTrip should delete the found trip and return no content`() {
        val existingTrip = trip.copy(id = 7L, trackableId = "track-123", slug = "to-delete")

        whenever(tripRepository.findBySlugAndTrackableId(eq("to-delete"), eq("track-123")))
            .thenReturn(Mono.just(existingTrip))
        val captor = argumentCaptor<Trip>()
        whenever(tripRepository.delete(captor.capture()))
            .thenReturn(Mono.empty())

        val result = controller.deleteTrip("track-123", "to-delete")

        StepVerifier.create(result)
            .consumeNextWith { response ->
                assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
            .verifyComplete()

        // only the trip itself is deleted; the exact entity that was looked up
        assertThat(captor.firstValue).isEqualTo(existingTrip)
        verify(tripRepository).delete(existingTrip)
    }

    @Test
    fun `markTripActive should clear the endDate and return no content`() {
        val existingTrip = trip.copy(
            id = 9L,
            trackableId = "track-123",
            slug = "ongoing",
            endDate = Instant.parse("2025-01-05T10:00:00Z")
        )

        whenever(tripRepository.findBySlugAndTrackableId(eq("ongoing"), eq("track-123")))
            .thenReturn(Mono.just(existingTrip))
        val captor = argumentCaptor<Trip>()
        whenever(tripRepository.save(captor.capture()))
            .thenAnswer { Mono.just(captor.firstValue) }

        val result = controller.markTripActive("track-123", "ongoing")

        StepVerifier.create(result)
            .consumeNextWith { response ->
                assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
            .verifyComplete()

        val saved = captor.firstValue
        // endDate cleared, everything else untouched
        assertThat(saved.endDate).isNull()
        assertThat(saved).usingRecursiveComparison().ignoringFields("endDate").isEqualTo(existingTrip)
    }

    @Test
    fun `markTripActive should throw not found when the trip does not exist`() {
        whenever(tripRepository.findBySlugAndTrackableId(eq("missing"), eq("track-123")))
            .thenReturn(Mono.empty())

        val result = controller.markTripActive("track-123", "missing")

        StepVerifier.create(result)
            .expectErrorSatisfies { error ->
                assertThat(error).isInstanceOf(ResponseStatusException::class.java)
                val ex = error as ResponseStatusException
                assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
                assertThat(ex.reason).isEqualTo("Trip not found")
            }
            .verify()

        verify(tripRepository, never()).save(any())
    }

    @Test
    fun `deleteTrip should throw not found when the trip does not exist`() {
        whenever(tripRepository.findBySlugAndTrackableId(eq("missing"), eq("track-123")))
            .thenReturn(Mono.empty())

        val result = controller.deleteTrip("track-123", "missing")

        StepVerifier.create(result)
            .expectErrorSatisfies { error ->
                assertThat(error).isInstanceOf(ResponseStatusException::class.java)
                val ex = error as ResponseStatusException
                assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
                assertThat(ex.reason).isEqualTo("Trip not found")
            }
            .verify()

        verify(tripRepository, never()).delete(any())
    }

    @Test
    fun `updateTrip should keep all fields unchanged when request has no values`() {
        val existingTrip = trip.copy(
            id = 43L,
            trackableId = "track-123",
            slug = "unchanged-slug",
            title = "Unchanged title",
            description = "Unchanged description",
            startDate = Instant.parse("2025-01-01T10:00:00Z"),
            endDate = Instant.parse("2025-01-01T10:00:00Z"),
            positionType = PositionType.ALL,
            color = "#abcdef",
            animationSpeed = 1234
        )

        val request = UpdateTripRequest()

        whenever(tripRepository.findBySlugAndTrackableId(eq("43"), eq("track-123")))
            .thenReturn(Mono.just(existingTrip))

        val captor = argumentCaptor<Trip>()
        whenever(tripRepository.save(captor.capture()))
            .thenAnswer { Mono.just(captor.firstValue) }

        val result = controller.updateTrip("track-123", "43", request)

        StepVerifier.create(result)
            .consumeNextWith { response ->
                assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            }
            .verifyComplete()

        val saved = captor.firstValue
        // all fields must stay identical
        assertThat(saved).usingRecursiveComparison().isEqualTo(existingTrip)
    }


}
