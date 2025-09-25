package com.snapiter.backend.api.trackable

import com.fasterxml.jackson.annotation.JsonFormat
import com.snapiter.backend.model.trackable.trip.PositionType
import com.snapiter.backend.model.trackable.trip.Trip
import com.snapiter.backend.model.trackable.trip.TripRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.time.LocalDateTime


@RestController
@RequestMapping("/api/trackables/{trackableId}")
@Tag(name = "Trackable Trips", description = "Endpoints for Trips")
@PreAuthorize("hasAnyRole('USER','DEVICE')")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "deviceToken")
class TripController(
    private val tripRepository: TripRepository
) {
    @PostMapping("/trips")
    @Operation(
        summary = "Create a trip for a trackable",
        description = "Creates a new trip under the specified trackable. Slug must be unique per trackable."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "Trip created successfully, no content returned"
            ),
            ApiResponse(
                responseCode = "400", description = "Validation error",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "409", description = "Duplicate slug for this trackable",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "500", description = "Server error",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            ),
        ]
    )
    fun createTrip(
        @PathVariable trackableId: String,
        @RequestBody @Valid body: CreateTripRequest
    ): Mono<ResponseEntity<Void>> {
        return tripRepository.findBySlugAndTrackableId(body.slug, trackableId)
            .switchIfEmpty(
                Mono.error(ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists for this trackable"))
            )
            .flatMap {
                val trip = Trip(
                    id = null,
                    trackableId = trackableId,
                    startDate = body.startDate,
                    endDate = body.endDate,
                    title = body.title,
                    description = body.description,
                    slug = body.slug,
                    positionType = body.positionType,
                    createdAt = LocalDateTime.now(),
                    color = if (body.color.startsWith("#")) body.color else "#${body.color}",
                    animationSpeed = body.animationSpeed
                )
                tripRepository.save(trip)
            }
            .then(Mono.just(ResponseEntity.noContent().build()))
    }

    @PutMapping("/trips/{trip}")
    @Operation(
        summary = "Update a trip",
        description = "Updates an existing trip. Fields set to null will be ignored (kept unchanged)."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "Trip updated successfully, no content returned"
            ),
            ApiResponse(
                responseCode = "400", description = "Validation error",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "404", description = "Trip not found",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "409", description = "Duplicate slug for this trackable",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "500", description = "Server error",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            ),
        ]
    )
    fun updateTrip(
        @PathVariable trackableId: String,
        @PathVariable trip: String,
        @RequestBody body: UpdateTripRequest
    ): Mono<ResponseEntity<Void>> {
        return tripRepository.findBySlugAndTrackableId(trip, trackableId)
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found")))
            .flatMap { existing ->
                val updated = existing.copy(
                    title = body.title ?: existing.title,
                    description = body.description ?: existing.description,
                    slug = body.slug ?: existing.slug,
                    startDate = body.startDate ?: existing.startDate,
                    endDate = body.endDate ?: existing.endDate,
                    positionType = body.positionType ?: existing.positionType,
                    color = body.color?.let { if (it.startsWith("#")) it else "#$it" } ?: existing.color,
                    animationSpeed = body.animationSpeed ?: existing.animationSpeed,
                )
                tripRepository.save(updated).thenReturn(ResponseEntity.noContent().build<Void>())
            }
    }
}


data class CreateTripRequest(
    @field:NotBlank @field:Size(max = 200)
    val title: String,

    @field:Size(max = 2000)
    val description: String? = null,

    @field:NotBlank @field:Pattern(regexp = "^[a-z0-9-]+$")
    val slug: String,

    // ISO strings; we store UTC
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    val startDate: LocalDateTime,

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    val endDate: LocalDateTime? = null,

    val positionType: PositionType = PositionType.HOURLY,
    @field:Pattern(regexp = "^#?[A-Fa-f0-9]{6}$")
    val color: String = "#648192",

    val animationSpeed: Long = 10_000
)

data class UpdateTripRequest(
    @field:Size(max = 200)
    val title: String? = null,

    @field:Size(max = 2000)
    val description: String? = null,

    @field:Pattern(regexp = "^[a-z0-9-]+$")
    val slug: String? = null,

    // ISO strings; we store UTC
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    val startDate: LocalDateTime? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    val endDate: LocalDateTime? = null,

    val positionType: PositionType? = null,

    @field:Pattern(regexp = "^#?[A-Fa-f0-9]{6}$")
    val color: String? = null,

    val animationSpeed: Long? = null
)
