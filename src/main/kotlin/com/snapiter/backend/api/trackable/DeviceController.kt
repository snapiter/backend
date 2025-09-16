package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.devices.Device
import com.snapiter.backend.model.trackable.devices.DeviceService
import com.snapiter.backend.security.UserPrincipal
import com.snapiter.backend.util.Qr
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/trackables/{trackableId}/devices")
@Tag(name = "Devices", description = "Manage devices for a trackable entity")
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
class DeviceController(
    private val deviceService: DeviceService
) {
    @PostMapping
    @Operation(
        summary = "Create a new device for a trackable",
        description = "Registers a new device under the specified trackableId."
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201", description = "Device created",
            content = [Content(schema = Schema(implementation = Device::class))]
        ),
        ApiResponse(responseCode = "400", description = "Invalid input", content = [Content()])
    )
    @PreAuthorize("@trackableAccessChecker.canAccess(#trackableId, authentication)")
    fun create(
        @PathVariable trackableId: String,
        @AuthenticationPrincipal user: UserPrincipal
    ): Mono<QuickCreateRes> {
        val deviceId = UUID.randomUUID().toString()
        return deviceService.issueDevice(
            trackableId,
            deviceId
        ).map { issued ->
            val payload = """{"deviceId":"$deviceId","token":"$issued"}"""
            val qr = Qr.dataUrl(payload) // see util below
            QuickCreateRes(
                deviceToken = issued,
                qrDataUrl = qr,
            )
        }
    }

    @GetMapping("/{deviceId}")
    @Operation(
        summary = "Get a device by deviceId",
        description = "Returns the device registered for the given trackableId and deviceId."
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200", description = "Device found",
            content = [Content(schema = Schema(implementation = Device::class))]
        ),
        ApiResponse(responseCode = "404", description = "Device not found", content = [Content()])
    )
    @PreAuthorize("@trackableAccessChecker.canAccess(#trackableId, authentication)")
    fun getDevice(
        @Parameter(description = "Trackable ID that owns the device")
        @PathVariable trackableId: String,
        @Parameter(description = "Device ID to look up")
        @PathVariable deviceId: String
    ): Mono<ResponseEntity<Device>> {
        return deviceService.getDevice(trackableId, deviceId)
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }

    @DeleteMapping("/{deviceId}")
    @Operation(
        summary = "Delete a device",
        description = "Deletes the device registered for the given trackableId and deviceId."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Device deleted", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Device not found", content = [Content()])
    )
    @PreAuthorize("@trackableAccessChecker.canAccess(#trackableId, authentication)")
    fun deleteDevice(
        @Parameter(description = "Trackable ID that owns the device")
        @PathVariable trackableId: String,
        @Parameter(description = "Device ID to delete")
        @PathVariable deviceId: String
    ): Mono<ResponseEntity<Void>> {
        return deviceService.deleteDevice(trackableId, deviceId)
            .map { deleted ->
                if (deleted) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
            }
    }
}
data class QuickCreateRes(
    val deviceToken: String,
    val qrDataUrl: String,
)