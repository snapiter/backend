package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.devices.Device
import com.snapiter.backend.model.trackable.devices.DeviceService
import com.snapiter.backend.model.trackable.devices.tokens.DeviceTokenService
import com.snapiter.backend.model.trackable.devices.tokens.UnauthorizedTokenException
import com.snapiter.backend.util.Qr
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/trackables/{trackableId}/devices")
@Tag(name = "Devices", description = "Manage devices for a trackable entity")
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
class DeviceController(
    private val deviceService: DeviceService,
    private val deviceTokenService: DeviceTokenService,
) {
    @PostMapping("/token")
    @Operation(
        summary = "Issue a token",
        description = "Issue a token to register a new device"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201", description = "Device token created",
            content = [Content(schema = Schema(implementation = QuickCreateRes::class))]
        ),
        ApiResponse(responseCode = "400", description = "Invalid input", content = [Content()])
    )
    @PreAuthorize("hasRole('USER')")
    fun issueToken(
        @PathVariable trackableId: String
    ): Mono<QuickCreateRes> {
        return deviceTokenService.issue(trackableId)
        .map { issued ->
            val payload = """{"trackableId":"$trackableId","token":"$issued"}"""
            val qr = Qr.dataUrl(payload) // see util below
            QuickCreateRes(
                deviceToken = issued,
                qrDataUrl = qr,
            )
        }
    }

    @PostMapping("/register")
    @Operation(
        summary = "Register a device",
        description = "Register a device with a token"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201", description = "Device registered",
            content = [Content(schema = Schema(implementation = QuickCreateRes::class))]
        ),
        ApiResponse(responseCode = "400", description = "Invalid input", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Trying to register with an token that either has been claimed or doesnt exist.", content = [Content()])
    )
    fun registerDevice(
        @PathVariable trackableId: String,
        @RequestBody req: RegisterDevice
    ): Mono<Device> {
        return deviceTokenService.validate(req.token)
            .filter { it.trackableId == trackableId } // only allow correct trackable
            .switchIfEmpty(Mono.error(UnauthorizedTokenException("Invalid trackableId for token")))
            .flatMap {
                deviceService.createDevice(it, req.deviceId, req.name)
            }
    }

    @GetMapping("")
    @Operation(
        summary = "Return all devices",
        description = "Returns all registered devices for the given trackableId."
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200", description = "Devices found",
            content = [Content(array = ArraySchema(schema = Schema(implementation = Device::class)))]
        ),
    )
    @PreAuthorize("@trackableAccessChecker.canAccess(#trackableId, authentication)")
    fun getDevices(
        @Parameter(description = "Trackable ID that owns the devices")
        @PathVariable trackableId: String,
    ): Mono<ResponseEntity<List<Device>>> {
        return deviceService.getDevices(trackableId)
            .collectList()
            .map { devices ->
                ResponseEntity.ok(devices)
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

data class RegisterDevice(
    @field:NotBlank
    val deviceId: String,
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val token: String,
)

