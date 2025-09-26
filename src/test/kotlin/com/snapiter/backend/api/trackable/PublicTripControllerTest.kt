package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.markers.Marker
import com.snapiter.backend.model.trackable.markers.MarkerRepository
import com.snapiter.backend.model.trackable.positionreport.PositionService
import com.snapiter.backend.model.trackable.trip.PositionType
import com.snapiter.backend.model.trackable.trip.Trip
import com.snapiter.backend.model.trackable.trip.TripRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.time.Duration

class PublicTripControllerTest {

    private val markerRepository = mock<MarkerRepository>()
    private val tripRepository = mock<TripRepository>()
    private val positionService = mock<PositionService>()

    private val controller = PublicTripController(positionService, markerRepository, tripRepository)

    @Test
    fun `should return markers for trip`() {
        val trip = Trip(
            id = 1L,
            trackableId = "trackableId123",
            startDate = Instant.parse("2019-04-12T02:01:00Z"),
            endDate = Instant.parse("2020-04-12T02:01:00Z"),
            title = "Test Trip",
            description = "Test Description",
            slug = "tripSlug",
            positionType = PositionType.HOURLY,
            createdAt = Instant.now(),
            color = "#648192",
            animationSpeed = 10000
        )

        whenever(tripRepository.findBySlugAndTrackableId("tripSlug", "trackableId123"))
            .thenReturn(Mono.just(trip))

        whenever(
            markerRepository.findAllByTrackableIdAndCreatedAtIsBetweenOrderByCreatedAtDesc(
                eq("trackableId123"),
                eq(trip.startDate),
                eq(trip.endDate!!)
            )
        ).thenReturn(
            Flux.just(
                Marker(
                    id = 23,
                    trackableId = "trackableId123",
                    markerId = "imageId",
                    fileSize = 100,
                    fileType = "",
                    hasThumbnail = false,
                    latitude = 1.0,
                    longitude = 2.0,
                    title = "Test",
                    description = "Desc",
                    createdAt = Instant.now()
                )
            )
        )

        StepVerifier.create(controller.getMarkers("trackableId123", "tripSlug"))
            .expectNextMatches {
                assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
                true
            }
            .expectComplete()
            .verify()
    }



    @Test
    fun `should fallback trip end date to NOW when endDate is null`() {
        val startDate = Instant.parse("2019-04-12T02:01:00Z")
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


        whenever(tripRepository.findBySlugAndTrackableId("tripSlug", "trackableId123"))
            .thenReturn(Mono.just(trip))

        whenever(
            markerRepository.findAllByTrackableIdAndCreatedAtIsBetweenOrderByCreatedAtDesc(
                eq("trackableId123"),
                eq(startDate),
                any()
            )
        ).thenReturn(
            Flux.just(
                Marker(
                    id = 42,
                    trackableId = "trackableId123",
                    markerId = "imageId2",
                    fileSize = 200,
                    fileType = "png",
                    hasThumbnail = true,
                    latitude = 3.0,
                    longitude = 4.0,
                    title = "Another",
                    description = "Another desc",
                    createdAt = Instant.now()
                )
            )
        )

        StepVerifier.create(controller.getMarkers("trackableId123", "tripSlug"))
            .expectNextMatches {
                assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
                true
            }
            .expectComplete()
            .verify()
    }

}
