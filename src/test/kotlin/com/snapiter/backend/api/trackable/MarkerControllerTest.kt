package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.markers.Marker
import com.snapiter.backend.model.trackable.markers.MarkerRepository
import com.snapiter.backend.util.s3.S3FileUpload
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDateTime

class MarkerControllerTest {

    private val markerRepository = mock<MarkerRepository>()
    private val fileService = mock<S3FileUpload>()

    private val controller = MarkerController(fileService, markerRepository)

    val marker = Marker(
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
        createdAt = LocalDateTime.now()
    );

    @Test
    fun `updateMarker should update fields when request has values`() {
        val request = UpdateMarkerRequest(
            latitude = 30.0,
            longitude = 40.0,
            title = "New Title",
            description = "New Desc",
            createdAt = marker.createdAt
        )

        val updatedMarker = marker.copy(
            latitude = request.latitude!!,
            longitude = request.longitude!!,
            title = request.title!!,
            description = request.description!!,
            createdAt = marker.createdAt
        )

        whenever(markerRepository.findByMarkerIdAndTrackableId(marker.markerId, marker.trackableId))
            .thenReturn(Mono.just(marker))
        whenever(markerRepository.save(any()))
            .thenReturn(Mono.just(updatedMarker))

        val result = controller.updateMarker(marker.trackableId, marker.markerId, request)

        StepVerifier.create(result)
            .expectNextMatches { response ->
                response.statusCode == HttpStatus.OK &&
                        response.body?.title == "New Title" &&
                        response.body?.description == "New Desc" &&
                        response.body?.latitude == 30.0 &&
                        response.body?.longitude == 40.0
            }
            .verifyComplete()
    }

    @Test
    fun `updateMarker should return not found when marker does not exist`() {
        val trackableId = "track-123"
        val markerId = "marker-999"

        whenever(markerRepository.findByMarkerIdAndTrackableId(markerId, trackableId))
            .thenReturn(Mono.empty())

        val request = UpdateMarkerRequest(
            latitude = null,
            longitude = null,
            title = null,
            description = null,
            createdAt = null
        )

        val result = controller.updateMarker(trackableId, markerId, request)

        StepVerifier.create(result)
            .expectNextMatches { response ->
                response.statusCode == HttpStatus.NOT_FOUND
            }
            .verifyComplete()
    }

    @Test
    fun `updateMarker should ignore empty title and description`() {
        val request = UpdateMarkerRequest(
            latitude = null,
            longitude = null,
            title = null,
            description = null,
            createdAt = null
        )

        whenever(markerRepository.findByMarkerIdAndTrackableId(marker.markerId, marker.trackableId))
            .thenReturn(Mono.just(marker))
        whenever(markerRepository.save(any()))
            .thenAnswer { invocation -> Mono.just(invocation.arguments[0] as Marker) }

        val result = controller.updateMarker(marker.trackableId, marker.markerId, request)

        StepVerifier.create(result)
            .expectNextMatches { response ->
                response.statusCode == HttpStatus.OK &&
                        response.body?.title == marker.title &&
                        response.body?.description == marker.description &&
                        response.body?.latitude == marker.latitude &&
                        response.body?.longitude == marker.longitude &&
                        response.body?.createdAt == marker.createdAt
            }
            .verifyComplete()
    }


    @Test
    fun `updateMarker should update empty string for title and description`() {
        val request = UpdateMarkerRequest(
            latitude = 0.0,
            longitude = 0.0,
            title = "",
            description = "",
            createdAt = null
        )

        whenever(markerRepository.findByMarkerIdAndTrackableId(marker.markerId, marker.trackableId))
            .thenReturn(Mono.just(marker))
        whenever(markerRepository.save(any()))
            .thenAnswer { invocation -> Mono.just(invocation.arguments[0] as Marker) }

        val result = controller.updateMarker(marker.trackableId, marker.markerId, request)

        StepVerifier.create(result)
            .expectNextMatches { response ->
                response.statusCode == HttpStatus.OK &&
                        response.body?.title == request.title &&
                        response.body?.description == request.description &&
                        response.body?.latitude == request.latitude &&
                        response.body?.longitude == request.longitude
            }
            .verifyComplete()
    }
}
