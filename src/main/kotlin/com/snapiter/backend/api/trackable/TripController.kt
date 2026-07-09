package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.trip.PositionType
import com.snapiter.backend.model.trackable.trip.TripService
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
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.Instant


@RestController
@RequestMapping("/api/trackables/{trackableId}")
@Tag(name = "Trackable Trips", description = "Endpoints for Trips")
@PreAuthorize("hasAnyRole('USER','DEVICE')")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "deviceToken")
class TripController(
    private val tripService: TripService
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
        @Valid @RequestBody body: CreateTripRequest
    ): Mono<ResponseEntity<Void>> {
        return tripService.createTrip(trackableId, body)
            .thenReturn(ResponseEntity.noContent().build())
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
        @Valid @RequestBody body: UpdateTripRequest
    ): Mono<ResponseEntity<Void>> {
        return tripService.updateTrip(trackableId, trip, body)
            .thenReturn(ResponseEntity.noContent().build())
    }

    @PutMapping("/trips/{trip}/active")
    @Operation(
        summary = "Set a trip as active",
        description = "Clears the trip's endDate, marking it as ongoing. Idempotent: an already " +
            "active trip stays active. To give the trip an endDate again, use the update endpoint."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "Trip marked active, no content returned"
            ),
            ApiResponse(
                responseCode = "404", description = "Trip not found",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "500", description = "Server error",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            ),
        ]
    )
    fun markTripActive(
        @PathVariable trackableId: String,
        @PathVariable trip: String
    ): Mono<ResponseEntity<Void>> {
        return tripService.markTripActive(trackableId, trip)
            .thenReturn(ResponseEntity.noContent().build())
    }

    @PutMapping("/trips/{trip}/end")
    @Operation(
        summary = "End a trip",
        description = "Marks the trip as ended by setting its endDate to the current time. " +
            "Idempotent: a trip that already has an endDate keeps its original end time. " +
            "To change an existing end time, use the update endpoint."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "Trip ended, no content returned"
            ),
            ApiResponse(
                responseCode = "404", description = "Trip not found",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "500", description = "Server error",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            ),
        ]
    )
    fun endTrip(
        @PathVariable trackableId: String,
        @PathVariable trip: String
    ): Mono<ResponseEntity<Void>> {
        return tripService.endTrip(trackableId, trip)
            .thenReturn(ResponseEntity.noContent().build())
    }

    @DeleteMapping("/trips/{trip}")
    @Operation(
        summary = "Delete a trip",
        description = "Deletes a single trip. Positions and markers belong to the trackable, " +
            "not the trip, and are left untouched."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "Trip deleted successfully, no content returned"
            ),
            ApiResponse(
                responseCode = "404", description = "Trip not found",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "500", description = "Server error",
                content = [Content(schema = Schema(implementation = org.springframework.http.ProblemDetail::class))]
            ),
        ]
    )
    fun deleteTrip(
        @PathVariable trackableId: String,
        @PathVariable trip: String
    ): Mono<ResponseEntity<Void>> {
        return tripService.deleteTrip(trackableId, trip)
            .thenReturn(ResponseEntity.noContent().build())
    }
}


data class CreateTripRequest(
    @field:NotBlank @field:Size(max = 200)
    val title: String,

    @field:Size(max = 2000)
    val description: String? = null,

    @field:NotBlank @field:Pattern(regexp = "^[a-z0-9-]+$")
    val slug: String,

    // ISO strings in UTC format (e.g., "2025-01-15T10:30:00Z")
    val startDate: Instant,

    val endDate: Instant? = null,

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

    // ISO strings in UTC format (e.g., "2025-01-15T10:30:00Z")
    val startDate: Instant? = null,
    val endDate: Instant? = null,

    val positionType: PositionType? = null,

    @field:Pattern(regexp = "^#?[A-Fa-f0-9]{6}$")
    val color: String? = null,

    val animationSpeed: Long? = null
)
