package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.markers.Marker
import com.snapiter.backend.model.trackable.markers.MarkerRepository
import com.snapiter.backend.security.AppPrincipal
import com.snapiter.backend.util.s3.S3FileUpload
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.http.codec.multipart.Part
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors

@RestController
@RequestMapping("/api/trackables/{trackableId}/markers")
@Tag(name = "Markers", description = "Manage markers for a trackable entity")
@PreAuthorize("hasAnyRole('USER', 'DEVICE')")
@SecurityRequirement(name = "deviceToken")
@SecurityRequirement(name = "bearerAuth")
class MarkerController(
    private val s3FileUpload: S3FileUpload,
    private val markerRepository: MarkerRepository
) {
    @PutMapping("{markerId}")
    @Operation(
        summary = "Update values of a marker",
        description = "Update title and descriptions of a marker"
    )
    @ApiResponse(responseCode = "200", description = "Marker updated")
    @ApiResponse(responseCode = "400", description = "Invalid marker id")
    fun updateMarker(
        @PathVariable trackableId: String,
        @PathVariable markerId: String,
        @RequestBody request: UpdateMarkerRequest
    ): Mono<ResponseEntity<Marker>> {
            return markerRepository.findByMarkerIdAndTrackableId(markerId, trackableId)
                .flatMap { marker ->
                    markerRepository.save(
                        marker.copy(
                            latitude = request.latitude ?: marker.latitude,
                            longitude = request.longitude ?: marker.longitude,
                            title = request.title?.takeIf { it.isNotEmpty() } ?: marker.title,
                            description = request.description?.takeIf { it.isNotEmpty() } ?: marker.description,
                            createdAt = request.createdAt ?: marker.createdAt
                        )
                    ).map { ResponseEntity.ok(it) }
                }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }

    @DeleteMapping("{markerId}")
    @Operation(
        summary = "Delete a marker",
        description = "Completely remove a marker"
    )
    @ApiResponse(
        responseCode = "202", description = "The marker is deleted"
    )
    fun deleteMarker(
        @PathVariable trackableId: String,
        @PathVariable markerId: String,
        @AuthenticationPrincipal p: AppPrincipal
    ): Mono<ResponseEntity<Void>> {
        return markerRepository.findByMarkerIdAndTrackableId(markerId, trackableId)
            .flatMap {
                markerRepository.delete(it)
                    .then(Mono.just(ResponseEntity.noContent().build<Void>()))
            }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }

    @PostMapping("/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Upload the marker image",
        description = "Upload the marker image. NOT sure if this works."
    )
    fun uploadImage(
        @PathVariable trackableId: String,
        @RequestBody parts: Flux<Part>
    ): Mono<ResponseEntity<String>> {
        val fileId = UUID.randomUUID()

        return parts.collectList().flatMap { allParts ->
            val fields = allParts.filterIsInstance<FormFieldPart>()
                .associate { it.name() to it.value() }

            val latitude = fields["latitude"]?.toDoubleOrNull()
            val longitude = fields["longitude"]?.toDoubleOrNull()

            if (latitude == null || longitude == null) {
                return@flatMap Mono.error<ResponseEntity<String>>(
                    IllegalArgumentException("Latitude/Longitude missing in form-data")
                )
            }

            val fileParts = allParts.filterIsInstance<FilePart>()

            Flux.fromIterable(fileParts)
                .flatMap { part ->
                    s3FileUpload.saveFile(fileId, "markers/", part, trackableId)
                }
                .then(
                    Mono.fromFuture {
                        s3FileUpload.getHeadObjectResponse(fileId.toString(), "images/")
                    }.map { head ->
                        Marker.create(
                            trackableId,
                            fileId.toString(),
                            latitude,
                            longitude,
                            head.contentLength(),
                            head.contentType()
                        )
                    }
                )
                .flatMap(markerRepository::save)
                .map { ResponseEntity.ok(fileId.toString()) }
        }
    }
}

data class UpdateMarkerRequest(
    val latitude: Double?,
    val longitude: Double?,
    val title: String?,
    val description: String?,
    val createdAt: LocalDateTime?
)