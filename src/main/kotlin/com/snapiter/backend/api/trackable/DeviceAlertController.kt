package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.devices.DeviceAlertService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/trackables/{trackableId}/devices/{deviceId}")
@Tag(name = "Devices", description = "Manage devices for a trackable entity")
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
class DeviceAlertController(
    private val deviceAlertService: DeviceAlertService
) {
    @PostMapping("/alert/battery")
    @Operation(
        summary = "Send a battery alert",
        description = "Sends a battery alert email to the owner of the trackable."
    )
    @ApiResponses(
        ApiResponse(responseCode = "202", description = "Alert accepted", content = [Content()]),
        ApiResponse(responseCode = "400", description = "Invalid input", content = [Content()])
    )
    @PreAuthorize("@trackableAccessChecker.canAccess(#trackableId, authentication)")
    fun batteryAlert(
        @Parameter(description = "Trackable ID that owns the device")
        @PathVariable trackableId: String,
        @Parameter(description = "Device ID that raised the alert")
        @PathVariable deviceId: String
    ): Mono<ResponseEntity<Void>> {
        return deviceAlertService.alert(trackableId, deviceId)
            .thenReturn(ResponseEntity.accepted().build())
    }
}
