package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.positionreport.PositionReport
import com.snapiter.backend.model.trackable.positionreport.PositionService
import com.snapiter.backend.model.trackable.trip.PositionType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/trackables")
@Tag(name = "Trackable", description = "Endpoint for devices to send their current geographic position.")
class PositionController(
    private val positionService: PositionService
) {
    @PostMapping("{trackableId}/{deviceId}/position")
    @Operation(
        summary = "Submit a position",
        description = "Accepts a single latitude/longitude update for the given device." +
                "An optional `createdAt` field can be provided; if omitted, the server timestamp will be used."
    )
    @ApiResponse(responseCode = "204", description = "Created")
    @ApiResponse(responseCode = "400", description = "Bad request")
    @ApiResponse(responseCode = "404", description = "Trackable/device not found")
    @ApiResponse(responseCode = "409", description = "Duplicate/conflict")
    fun createPositionReport(
        @PathVariable trackableId: String,
        @PathVariable deviceId: String,
        @RequestBody request: PositionRequest
    ): Mono<ResponseEntity<Void>> {
        return positionService.report(trackableId, deviceId, request)
            .thenReturn(ResponseEntity.noContent().build())
    }


    @GetMapping("/{trackableId}/positions")
    @ApiResponse(
        responseCode = "200", description = "Return a list of position reports",
        content = [Content(
            mediaType = "application/json",
            schema = Schema(implementation = PositionReport::class)
        )]
    )
    fun getPositions(
        @PathVariable trackableId: String,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fromDate: LocalDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) untilDate: LocalDateTime?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "500") size: Int
    ): Flux<PositionReport> {
        return positionService.positions(PositionType.ALL, trackableId, fromDate, untilDate, page, size)
    }
}

data class PositionRequest(
    @field:DecimalMin(value = "-90.0", message = "latitude must be >= -90")
    @field:DecimalMax(value = "90.0", message = "latitude must be <= 90")
    val latitude: Double,

    @field:DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @field:DecimalMax(value = "180.0", message = "longitude must be <= 180")
    val longitude: Double,

    // Optional client timestamp; if null, server time is used
    val createdAt: OffsetDateTime? = null
)
