package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.devices.Device
import com.snapiter.backend.model.trackable.devices.DeviceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/trackables/{trackableId}/devices")
@Tag(name = "Devices", description = "Manage devices for a trackable entity")
class DeviceController(
    private val deviceService: DeviceService
) {

    data class CreateDeviceRequest(
        val deviceId: String
    )

    @PostMapping
    @Operation(
        summary = "Create a new device for a trackable",
        description = "Registers a new device under the specified trackableId."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Device created",
            content = [Content(schema = Schema(implementation = Device::class))]),
        ApiResponse(responseCode = "400", description = "Invalid input", content = [Content()])
    )
    fun createDevice(
        @Parameter(description = "Trackable ID that owns the device")
        @PathVariable trackableId: String,
        @RequestBody req: CreateDeviceRequest
    ): Mono<ResponseEntity<Device>> {
        return deviceService.createDevice(trackableId, req.deviceId)
            .map { ResponseEntity.status(HttpStatus.CREATED).body(it) }
    }

    @GetMapping("/{deviceId}")
    @Operation(
        summary = "Get a device by deviceId",
        description = "Returns the device registered for the given trackableId and deviceId."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Device found",
            content = [Content(schema = Schema(implementation = Device::class))]),
        ApiResponse(responseCode = "404", description = "Device not found", content = [Content()])
    )
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
