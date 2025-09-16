package com.snapiter.backend.api.trackable
import com.snapiter.backend.model.trackable.devices.DeviceService
import com.snapiter.backend.security.UserPrincipal
import com.snapiter.backend.util.Qr
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/api/trackables/{trackableId}/devices")
@SecurityRequirement(name = "bearerAuth") // user must be logged in
class DeviceQuickController(
    private val deviceService: DeviceService
) {
    @PostMapping("/quick-qr")
    @Operation(
        summary = "Create device + QR",
        description = "Creates a device, mints a device token, returns a QR with {deviceId, token}.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun quickCreate(
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
}
data class QuickCreateRes(
    val deviceToken: String,   // show once, put in QR
    val qrDataUrl: String,     // "data:image/png;base64,..."
)