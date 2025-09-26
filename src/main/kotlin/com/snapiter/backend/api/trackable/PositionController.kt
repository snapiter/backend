package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.positionreport.PositionReport
import com.snapiter.backend.model.trackable.positionreport.PositionService
import com.snapiter.backend.model.trackable.trip.PositionType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@RestController
@RequestMapping("/api/trackables")
@Tag(name = "Trackable Positions", description = "Endpoint for devices to send their current geographic position.")
@PreAuthorize("hasAnyRole('DEVICE')")
@SecurityRequirement(name = "deviceToken")
class PositionController(
    private val positionService: PositionService
) {
    @PostMapping("/{trackableId}/{deviceId}/position")
    @Operation(
        summary = "Submit a position",
        description = "Accepts a single latitude/longitude update for the given device." +
                "An optional `createdAt` field can be provided; if omitted, the server timestamp will be used.",
        security = [
            SecurityRequirement(name = "deviceToken")
        ]
    )
    @ApiResponse(responseCode = "204", description = "Created")
    @ApiResponse(responseCode = "400", description = "Bad request")
    @ApiResponse(responseCode = "404", description = "Trackable/device not found")
    @ApiResponse(responseCode = "409", description = "Duplicate/conflict")
    @PreAuthorize("hasRole('DEVICE')")
    @SecurityRequirement(name = "deviceToken")
    fun createPositionReport(
        @PathVariable trackableId: String,
        @PathVariable deviceId: String,
        @RequestBody request: PositionRequest
    ): Mono<ResponseEntity<Void>> {
        return positionService.report(trackableId, deviceId, listOf(request))
            .then(Mono.just(ResponseEntity.noContent().build()))
    }

    @PostMapping("/{trackableId}/{deviceId}/positions")
    @Operation(
        summary = "Submit multiple positions",
        description = "Accepts a list of latitude/longitude updates for the given device. " +
                "Each item may include an optional `createdAt` field; if omitted, the server timestamp will be used.",
        security = [SecurityRequirement(name = "deviceToken")]
    )
    @ApiResponse(responseCode = "204", description = "Created")
    @ApiResponse(responseCode = "400", description = "Bad request")
    @ApiResponse(responseCode = "404", description = "Trackable/device not found")
    @ApiResponse(responseCode = "409", description = "Duplicate/conflict")
    @PreAuthorize("hasRole('DEVICE')")
    @SecurityRequirement(name = "deviceToken")
    fun createPositionReports(
        @PathVariable trackableId: String,
        @PathVariable deviceId: String,
        @RequestBody requests: List<PositionRequest>
    ): Mono<ResponseEntity<Void>> {
        return positionService.report(trackableId, deviceId, requests)
            .then(Mono.just(ResponseEntity.noContent().build()))
    }
}

data class PositionRequest(
    @field:DecimalMin(value = "-90.0", message = "latitude must be >= -90")
    @field:DecimalMax(value = "90.0", message = "latitude must be <= 90")
    val latitude: Double,

    @field:DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @field:DecimalMax(value = "180.0", message = "longitude must be <= 180")
    val longitude: Double,

    // Optional client timestamp in UTC format (e.g., "2025-01-15T10:30:00Z"); if null, server time is used
    val createdAt: Instant? = null
)
