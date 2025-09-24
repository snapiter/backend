package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.markers.Marker
import com.snapiter.backend.model.trackable.markers.MarkerRepository
import com.snapiter.backend.util.s3.FileResponseWrapperService
import com.snapiter.backend.util.thumbnail.ThumbnailSize
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

@RestController
@RequestMapping("/api/trackables/{trackableId}")
@Tag(name = "Markers", description = "Manage markers for a trackable entity")
class PublicMarkerController(
    private val markerRepository: MarkerRepository,
    private val fileResponseWrapperService: FileResponseWrapperService
) {
    @GetMapping("/markers/{markerId}")
    @ApiResponse(responseCode = "404", description = "Could not find marker")
    fun getMarker(
        @PathVariable trackableId: String,
        @PathVariable markerId: String
    ): Mono<ResponseEntity<Marker>> {
        return markerRepository.findByMarkerIdAndTrackableId(markerId, trackableId)
            .map { marker -> ResponseEntity.ok(marker) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }

    @GetMapping("/markers/{markerId}/thumbnail/{size}")
    @ApiResponse(responseCode = "404", description = "Could not find image")
    fun thumbnailMarker(
        @PathVariable trackableId: String,
        @PathVariable markerId: String,
        @PathVariable size: String
    ): Mono<ResponseEntity<Flux<ByteBuffer>>> {
        val thumbnailSize = ThumbnailSize.fromValue(size)
        if (thumbnailSize == null) {
            return Mono.just(ResponseEntity.notFound().build())
        }

        return markerRepository.findByMarkerIdAndTrackableId(markerId, trackableId).flatMap { marker ->
            if (!marker.hasThumbnail) {
                Mono.just(ResponseEntity.notFound().build())
            } else {
                Mono.just(
                    fileResponseWrapperService.previewFile(
                        trackableId,
                        markerId,
                        marker.fileType,
                        "markers/thumbnails/${markerId}/${size}"
                    )
                )
            }
        }.switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }




    @GetMapping("/markers/{markerId}/image")
    @Operation(
        summary = "Get the full resolution image of a marker",
        description = "Retrieves the full-size image associate. The image is returned in its original resolution and format."
    )

    @ApiResponse(responseCode = "404", description = "Could not find image")
    fun previewMarker(
        @PathVariable trackableId: String,
        @PathVariable markerId: String
    ): Mono<ResponseEntity<Flux<ByteBuffer>>> {
        return markerRepository.findByMarkerIdAndTrackableId(markerId, trackableId).map {
            fileResponseWrapperService.previewFile(
                trackableId,
                markerId,
                it.fileType,
                "markers/$markerId"
            )
        }.defaultIfEmpty(ResponseEntity.notFound().build())
    }
}
