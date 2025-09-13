package com.snapiter.backend.api.trackable

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
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


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
    fun getTrips(
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
}