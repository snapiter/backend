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
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDateTime

class TripControllerTest {
    private val tripRepository: TripRepository = mock()
    private val controller = TripController(tripRepository)

    val trip = Trip(
        id = 2L,
        trackableId = "trackableId123",
        startDate = LocalDateTime.of(2019, 4, 12, 2, 1, 0),
        endDate = null,
        title = "Trip Without End",
        description = null,
        slug = "tripSlug",
        positionType = PositionType.HOURLY,
        createdAt = LocalDateTime.now(),
        color = "#648192",
        animationSpeed = 10000
    )

    @Test
    fun `should create and return no content when slug not found`() {
        // given
        val request = CreateTripRequest(
            startDate = LocalDateTime.of(2025, 1, 1, 10, 0),
            endDate = LocalDateTime.of(2025, 1, 2, 10, 0),
            title = "My Trip",
            description = "Trip description",
            slug = "my-trip",
            positionType = PositionType.ALL,
            color = "123456",
            animationSpeed = 2
        )

        whenever(tripRepository.findBySlugAndTrackableId(eq(request.slug), eq("track-123")))
            .thenReturn(Mono.just(trip)) // found -> should trigger conflict error
        whenever(tripRepository.save(any()))
            .thenReturn(Mono.just(Trip(id = 2L, trackableId = "track-123", startDate = request.startDate,
                endDate = request.endDate, title = request.title, description = request.description,
                slug = request.slug, positionType = request.positionType, createdAt = LocalDateTime.now(),
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
            startDate = LocalDateTime.now(),
            endDate = LocalDateTime.now().plusDays(1),
            title = "Existing trip",
            description = "desc",
            slug = "existing",
            positionType = PositionType.ALL,
            color = "#abcdef",
            animationSpeed = 1
        )

        whenever(tripRepository.findBySlugAndTrackableId(eq(request.slug), eq("track-123")))
            .thenReturn(Mono.empty()) // simulate that it should error

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
            startDate = LocalDateTime.of(2020, 1, 1, 0, 0),
            endDate = LocalDateTime.of(2020, 2, 1, 0, 0),
            positionType = PositionType.HOURLY,
            color = "#abcdef",
            animationSpeed = 5000
        )

        val request = UpdateTripRequest(
            title = "New title",
            description = "New description",
            slug = "new-slug",
            startDate = LocalDateTime.of(2030, 1, 1, 0, 0),
            endDate = LocalDateTime.of(2030, 2, 1, 0, 0),
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
        assertThat(saved.startDate).isEqualTo(LocalDateTime.of(2030, 1, 1, 0, 0))
        assertThat(saved.endDate).isEqualTo(LocalDateTime.of(2030, 2, 1, 0, 0))
        assertThat(saved.positionType).isEqualTo(PositionType.ALL)
        assertThat(saved.color).isEqualTo("#123456")
        assertThat(saved.animationSpeed).isEqualTo(9999L)

        // unchanged
        assertThat(saved.id).isEqualTo(existingTrip.id)
        assertThat(saved.trackableId).isEqualTo(existingTrip.trackableId)
        assertThat(saved.createdAt).isEqualTo(existingTrip.createdAt)
    }

    @Test
    fun `updateTrip should keep all fields unchanged when request has no values`() {
        val existingTrip = trip.copy(
            id = 43L,
            trackableId = "track-123",
            slug = "unchanged-slug",
            title = "Unchanged title",
            description = "Unchanged description",
            startDate = LocalDateTime.of(2020, 3, 1, 0, 0),
            endDate = LocalDateTime.of(2020, 4, 1, 0, 0),
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
