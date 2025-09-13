package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.devices.DeviceRepository
import com.snapiter.backend.model.trackable.positionreport.PositionReport
import com.snapiter.backend.model.trackable.positionreport.PositionReportRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/trackable/")
@Tag(name = "Trackable", description = "Endpoint for devices to send their current geographic position.")
class PositionController(
    private val deviceRepository: DeviceRepository,
    private val positionReportRepository: PositionReportRepository,
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
        @RequestBody request: PositionReportRequest
    ): Mono<ResponseEntity<Void>> {
        return deviceRepository.findByDeviceIdAndTrackableId(deviceId, trackableId)
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found for trackable")))
            .flatMap { device ->
                val positionReportMono = if (request.createdAt != null) {
                    positionReportRepository.save(
                        PositionReport.createFromLatAndLong(
                            trackableId,
                            request.latitude,
                            request.longitude,
                            request.createdAt
                        )
                    )
                } else {
                    positionReportRepository.save(
                        PositionReport.createFromLatAndLong(
                            trackableId,
                            request.latitude,
                            request.longitude
                        )
                    )
                }
                positionReportMono.map {
                    ResponseEntity.noContent().build<Void>()
                }
        }.defaultIfEmpty(ResponseEntity.notFound().build())
    }
}

data class PositionReportRequest(
    @field:DecimalMin(value = "-90.0", message = "latitude must be >= -90")
    @field:DecimalMax(value = "90.0", message = "latitude must be <= 90")
    val latitude: Double,

    @field:DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @field:DecimalMax(value = "180.0", message = "longitude must be <= 180")
    val longitude: Double,

    // Optional client timestamp; if null, server time is used
    val createdAt: OffsetDateTime? = null
)
