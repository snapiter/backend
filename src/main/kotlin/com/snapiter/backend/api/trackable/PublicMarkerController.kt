package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.markers.Marker
import com.snapiter.backend.model.trackable.markers.MarkerRepository
import com.snapiter.backend.model.trackable.trip.TripRepository
import com.snapiter.backend.util.s3.FileResponseWrapperService
import com.snapiter.backend.util.thumbnail.ThumbnailSize
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.nio.ByteBuffer
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/trackables/{trackableId}")
@Tag(name = "Markers", description = "Manage markers for a trackable entity")
class PublicMarkerController(
    private val markerRepository: MarkerRepository,
    private val tripRepository: TripRepository,
    private val fileResponseWrapperService: FileResponseWrapperService
) {
    @GetMapping("/trips/{trip}/markers")
    @Operation(
        summary = "Get all markers for a trip",
        description = "Returns all markers for a specific trip by its slug for the given trackable."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Markers found",
                content = [Content(array = ArraySchema(schema = Schema(implementation = Marker::class)))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Markers not found",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            )
        ]
    )
    fun getMarkers(
        @PathVariable trackableId: String,
        @PathVariable trip: String,
    ): Mono<ResponseEntity<Flux<Marker>>> {
        return tripRepository.findBySlugAndTrackableId(trip, trackableId).map {
            markerRepository.findAllByTrackableIdAndCreatedAtIsBetweenOrderByCreatedAtDesc(
                trackableId,
                it.startDate,
                it.endDate ?: LocalDateTime.now()
            )
        }.flatMap {
            ResponseEntity.ok(it).toMono()
        }
        .defaultIfEmpty(ResponseEntity.ok(Flux.empty()))
    }

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

    @GetMapping("/markers/{trackableId}/{markerId}/thumbnail/{size}")
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
                        marker.fileSize,
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
                it.fileSize,
                "markers/$markerId"
            )
        }.defaultIfEmpty(ResponseEntity.notFound().build())
    }
}
