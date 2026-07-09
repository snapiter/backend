package com.snapiter.backend.model.trackable.trip

import com.snapiter.backend.api.trackable.CreateTripRequest
import com.snapiter.backend.api.trackable.UpdateTripRequest
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration
import java.time.Instant

class TripServiceTest {
    private val tripRepository: TripRepository = mock()
    private val service = TripService(tripRepository)

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
    fun `should return the trips based on trackable`() {
        val other = trip.copy(id = 3L, slug = "another")
        whenever(tripRepository.findAllByTrackableIdOrderByEndDateDescNullsFirst(eq("track-123")))
            .thenReturn(Flux.just(trip, other))

        val result = service.getTrips("track-123")

        StepVerifier.create(result)
            .expectNext(trip)
            .expectNext(other)
            .verifyComplete()
    }

    @Test
    fun `should return trip based on trackable id and trip slug`() {
        whenever(tripRepository.findBySlugAndTrackableId(eq("tripSlug"), eq("track-123")))
            .thenReturn(Mono.just(trip))

        val result = service.getTrip("track-123", "tripSlug")

        StepVerifier.create(result)
            .expectNext(trip)
            .verifyComplete()
    }

    @Test
    fun `should return empty result if trip is missing`() {
        whenever(tripRepository.findBySlugAndTrackableId(eq("missing"), eq("track-123")))
            .thenReturn(Mono.empty())

        val result = service.getTrip("track-123", "missing")

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should save trip when trip slug does not exist`() {
        val request = CreateTripRequest(
            startDate = Instant.parse("2025-01-01T10:00:00Z"),
            endDate = Instant.parse("2025-01-02T10:00:00Z"),
            title = "My Trip",
            description = "Trip description",
            slug = "my-trip",
            positionType = PositionType.ALL,
            color = "123456", // no # in input → should get prefixed
            animationSpeed = 2
        )

        whenever(tripRepository.findBySlugAndTrackableId(eq(request.slug), eq("track-123")))
            .thenReturn(Mono.empty())
        val captor = argumentCaptor<Trip>()
        whenever(tripRepository.save(captor.capture()))
            .thenAnswer { Mono.just(captor.firstValue) }

        val result = service.createTrip("track-123", request)

        StepVerifier.create(result)
            .consumeNextWith { saved ->
                assertThat(saved.slug).isEqualTo("my-trip")
            }
            .verifyComplete()

        val saved = captor.firstValue
        assertThat(saved.id).isNull()
        assertThat(saved.trackableId).isEqualTo("track-123")
        assertThat(saved.startDate).isEqualTo(request.startDate)
        assertThat(saved.endDate).isEqualTo(request.endDate)
        assertThat(saved.title).isEqualTo("My Trip")
        assertThat(saved.description).isEqualTo("Trip description")
        assertThat(saved.positionType).isEqualTo(PositionType.ALL)
        assertThat(saved.color).isEqualTo("#123456")
        assertThat(saved.animationSpeed).isEqualTo(2L)
        assertThat(saved.createdAt).isNotNull()
    }

    @Test
    fun `should not create trip when trip slug already exists`() {
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
            .thenReturn(Mono.just(trip))

        val result = service.createTrip("track-123", request)

        StepVerifier.create(result)
            .expectErrorSatisfies { error ->
                assertThat(error).isInstanceOf(ResponseStatusException::class.java)
                val ex = error as ResponseStatusException
                assertThat(ex.statusCode).isEqualTo(HttpStatus.CONFLICT)
                assertThat(ex.reason).isEqualTo("Slug already exists for this trackable")
            }
            .verify()

        verify(tripRepository, never()).save(any())
    }

    @Test
    fun `should update trip`() {
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
            startDate = Instant.parse("2025-01-02T10:00:00Z"),
            endDate = Instant.parse("2025-01-03T10:00:00Z"),
            positionType = PositionType.ALL,
            color = "123456", // no # in input → should get prefixed
            animationSpeed = 9999L
        )

        whenever(tripRepository.findBySlugAndTrackableId(eq("old-slug"), eq("track-123")))
            .thenReturn(Mono.just(existingTrip))
        val captor = argumentCaptor<Trip>()
        whenever(tripRepository.save(captor.capture()))
            .thenAnswer { Mono.just(captor.firstValue) }

        val result = service.updateTrip("track-123", "old-slug", request)

        StepVerifier.create(result)
            .expectNextCount(1)
            .verifyComplete()

        val saved = captor.firstValue
        assertThat(saved.title).isEqualTo("New title")
        assertThat(saved.description).isEqualTo("New description")
        assertThat(saved.slug).isEqualTo("new-slug")
        assertThat(saved.startDate).isEqualTo(Instant.parse("2025-01-02T10:00:00Z"))
        assertThat(saved.endDate).isEqualTo(Instant.parse("2025-01-03T10:00:00Z"))
        assertThat(saved.positionType).isEqualTo(PositionType.ALL)
        assertThat(saved.color).isEqualTo("#123456")
        assertThat(saved.animationSpeed).isEqualTo(9999L)

        // unchanged
        assertThat(saved.id).isEqualTo(existingTrip.id)
        assertThat(saved.trackableId).isEqualTo(existingTrip.trackableId)
        assertThat(saved.createdAt).isEqualTo(existingTrip.createdAt)
    }

    @Test
    fun `should keep unchanged fields when values are empty`() {
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

        whenever(tripRepository.findBySlugAndTrackableId(eq("unchanged-slug"), eq("track-123")))
            .thenReturn(Mono.just(existingTrip))
        val captor = argumentCaptor<Trip>()
        whenever(tripRepository.save(captor.capture()))
            .thenAnswer { Mono.just(captor.firstValue) }

        val result = service.updateTrip("track-123", "unchanged-slug", request)

        StepVerifier.create(result)
            .expectNextCount(1)
            .verifyComplete()

        assertThat(captor.firstValue).usingRecursiveComparison().isEqualTo(existingTrip)
    }

    @Test
    fun `should not update trip when the trip does not exist`() {
        whenever(tripRepository.findBySlugAndTrackableId(eq("missing"), eq("track-123")))
            .thenReturn(Mono.empty())

        val result = service.updateTrip("track-123", "missing", UpdateTripRequest())

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
    fun `should end the trip`() {
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

        val result = service.markTripActive("track-123", "ongoing")

        StepVerifier.create(result)
            .expectNextCount(1)
            .verifyComplete()

        val saved = captor.firstValue
        // endDate cleared, everything else untouched
        assertThat(saved.endDate).isNull()
        assertThat(saved).usingRecursiveComparison().ignoringFields("endDate").isEqualTo(existingTrip)
    }

    @Test
    fun `should not mark trip active when trip not found`() {
        whenever(tripRepository.findBySlugAndTrackableId(eq("missing"), eq("track-123")))
            .thenReturn(Mono.empty())

        val result = service.markTripActive("track-123", "missing")

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
    fun `should end trip`() {
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
        val result = service.endTrip("track-123", "to-end")

        StepVerifier.create(result)
            .expectNextCount(1)
            .verifyComplete()

        val saved = captor.firstValue
        assertThat(saved.endDate).isNotNull()
        assertThat(saved.endDate).isBetween(before, Instant.now())
        // only endDate changes
        assertThat(saved).usingRecursiveComparison().ignoringFields("endDate").isEqualTo(existingTrip)
    }

    @Test
    fun `should keep the endDate when trip already ended`() {
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

        val result = service.endTrip("track-123", "already-ended")

        StepVerifier.create(result)
            .expectNextCount(1)
            .verifyComplete()

        // idempotent: end time is unchanged
        assertThat(captor.firstValue.endDate).isEqualTo(originalEnd)
    }

    @Test
    fun `should not end trip when trip not found`() {
        whenever(tripRepository.findBySlugAndTrackableId(eq("missing"), eq("track-123")))
            .thenReturn(Mono.empty())

        val result = service.endTrip("track-123", "missing")

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
    fun `should delete trip`() {
        val existingTrip = trip.copy(id = 7L, trackableId = "track-123", slug = "to-delete")

        whenever(tripRepository.findBySlugAndTrackableId(eq("to-delete"), eq("track-123")))
            .thenReturn(Mono.just(existingTrip))
        whenever(tripRepository.delete(any()))
            .thenReturn(Mono.empty())

        val result = service.deleteTrip("track-123", "to-delete")

        StepVerifier.create(result)
            .verifyComplete()

        verify(tripRepository).delete(existingTrip)
    }

    @Test
    fun `should not delete trip found when trip is not found`() {
        whenever(tripRepository.findBySlugAndTrackableId(eq("missing"), eq("track-123")))
            .thenReturn(Mono.empty())

        val result = service.deleteTrip("track-123", "missing")

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
}
