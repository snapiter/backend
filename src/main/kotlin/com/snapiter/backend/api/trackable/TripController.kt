package com.snapiter.backend.api.trackable

import com.fasterxml.jackson.annotation.JsonFormat
import com.snapiter.backend.model.trackable.positionreport.PositionReport
import com.snapiter.backend.model.trackable.positionreport.PositionService
import com.snapiter.backend.model.trackable.trip.PositionType
import com.snapiter.backend.model.trackable.trip.Trip
import com.snapiter.backend.model.trackable.trip.TripRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime


@RestController
@RequestMapping("/api/trackables")
@Tag(name = "Trackable", description = "Endpoints for Trips")
class TripController(
    private val positionService: PositionService,
    private val tripRepository: TripRepository
) {
    @GetMapping("/{trackableId}/trips")
    @Operation(
        summary = "List trips for a trackable",
        description = "Returns all trips belonging to the specified trackable."
    )
    @ApiResponse(
        responseCode = "200", description = "Return a list of trips",
        content = [Content(
            mediaType = "application/json",
            schema = Schema(implementation = PositionReport::class)
        )]
    )
    fun getTrips(
        @PathVariable trackableId: String
    ): Flux<Trip> {
        return tripRepository.findAllByTrackableId(trackableId);
    }


    @GetMapping("/{trackableId}/trips/{trip}")
    @Operation(
        summary = "Get a specific trip by slug",
        description = "Returns a single trip by its slug for the given trackable."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Trip found",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = Trip::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Trip not found",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            )
        ]
    )
    fun getTrip(
        @PathVariable trackableId: String,
        @PathVariable trip: String,
    ): Mono<Trip> {
        return tripRepository.findBySlugAndTrackableId(trip, trackableId);
    }

    @GetMapping("/{trackableId}/trips/{trip}/positions")
    @ApiResponse(
        responseCode = "200", description = "Return a list of positions for that trip",
        content = [Content(
            mediaType = "application/json",
            schema = Schema(implementation = PositionReport::class)
        )]
    )
    fun getPositions(
        @PathVariable trackableId: String,
        @PathVariable trip: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "500") size: Int
    ): Flux<PositionReport> {
        return tripRepository.findBySlugAndTrackableId(trip, trackableId)
            .switchIfEmpty(
                Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"))
            )
            .flatMapMany { t ->
                positionService.positions(
                    t.positionType,
                    trackableId,
                    t.startDate,
                    t.endDate,
                    page,
                    size
                )
            }
    }

    @PostMapping("/{trackableId}/trips")
    @Operation(
        summary = "Create a trip for a trackable",
        description = "Creates a new trip under the specified trackable (vessel). Slug must be unique per trackable."
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