package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.devices.Device
import com.snapiter.backend.model.trackable.devices.DeviceService
import com.snapiter.backend.model.trackable.devices.tokens.DeviceTokenService
import com.snapiter.backend.model.trackable.devices.tokens.UnauthorizedTokenException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/trackables/{trackableId}/devices")
@Tag(name = "Devices", description = "Manage devices for a trackable entity")
class PublicDeviceController(
    private val deviceService: DeviceService,
    private val deviceTokenService: DeviceTokenService,
) {
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
        @Valid @RequestBody req: RegisterDevice
    ): Mono<Device> {
        return deviceTokenService.validate(req.token)
            .filter { it.trackableId == trackableId } // only allow correct trackable
            .switchIfEmpty(Mono.error(UnauthorizedTokenException("Invalid trackableId for token")))
            .flatMap {
                deviceService.createDevice(it, req.deviceId, req.name)
            }
    }
}

data class RegisterDevice(
    @field:NotBlank
    val deviceId: String,
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val token: String,
)

