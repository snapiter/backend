package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.markers.Marker
import com.snapiter.backend.model.trackable.markers.MarkerRepository
import com.snapiter.backend.util.s3.FileResponseWrapperService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDateTime

class PublicMarkerControllerTest {

    private val markerRepository = mock<MarkerRepository>()
    private val fileService = mock<FileResponseWrapperService>()

    private val controller = PublicMarkerController(markerRepository, fileService)

    @Test
    fun `should return marker`() {
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
        )

        whenever(markerRepository.findByMarkerIdAndTrackableId(eq("marker-123"), eq("track-456")))
            .thenReturn(Mono.just(marker))

        val result = controller.getMarker("track-456", "marker-123")

        StepVerifier.create(result)
            .expectNextMatches { response ->
                response.statusCode == HttpStatus.OK &&
                        response.body == marker
            }
            .verifyComplete()
    }

    @Test
    fun `should give 404 if marker not found`() {
        whenever(markerRepository.findByMarkerIdAndTrackableId(any(), any()))
            .thenReturn(Mono.empty())

        val result = controller.getMarker("track-456", "missing-marker")

        StepVerifier.create(result)
            .expectNextMatches { response ->
                response.statusCode == HttpStatus.NOT_FOUND &&
                        response.body == null
            }
            .verifyComplete()
    }
}
