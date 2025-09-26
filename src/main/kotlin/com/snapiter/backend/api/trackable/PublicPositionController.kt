package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.positionreport.PositionReport
import com.snapiter.backend.model.trackable.positionreport.PositionService
import com.snapiter.backend.model.trackable.trip.PositionType
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.time.Instant

@RestController
@RequestMapping("/api/trackables")
@Tag(name = "Trackable Positions", description = "Endpoint for devices to send their current geographic position.")
class PublicPositionController(
    private val positionService: PositionService
) {
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
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fromDate:
        Instant?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) untilDate: Instant?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "500") size: Int
    ): Flux<PositionReport> {
        return positionService.positions(PositionType.ALL, trackableId, fromDate, untilDate, page, size)
    }
}

