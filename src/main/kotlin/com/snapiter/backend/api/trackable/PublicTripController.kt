package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.markers.Marker
import com.snapiter.backend.model.trackable.markers.MarkerRepository
import com.snapiter.backend.model.trackable.positionreport.PositionReport
import com.snapiter.backend.model.trackable.positionreport.PositionService
import com.snapiter.backend.model.trackable.trip.Trip
import com.snapiter.backend.model.trackable.trip.TripRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Instant


@RestController
@RequestMapping("/api/trackables/{trackableId}")
@Tag(name = "Trackable Trips", description = "Endpoints for Trips")
class PublicTripController(
    private val positionService: PositionService,
    private val markerRepository: MarkerRepository,
    private val tripRepository: TripRepository
) {
    @GetMapping("/trips")
    @Operation(
        summary = "List trips for a trackable",
        description = "Returns all trips belonging to the specified trackable."
    )
    @ApiResponse(
        responseCode = "200", description = "Return a list of trips",
        content = [Content(array = ArraySchema(schema = Schema(implementation = Trip::class)))]
    )
    fun getTrips(
        @PathVariable trackableId: String
    ): Flux<Trip> {
        return tripRepository.findAllByTrackableId(trackableId);
    }


    @GetMapping("/trips/{trip}")
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

    @GetMapping("/trips/{trip}/positions")
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
                it.endDate ?: Instant.now()
            )
        }.flatMap {
            ResponseEntity.ok(it).toMono()
        }
        .defaultIfEmpty(ResponseEntity.ok(Flux.empty()))
    }
}