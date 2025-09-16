package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.trackable.Trackable
import com.snapiter.backend.model.trackable.trackable.TrackableService
import com.snapiter.backend.security.AppPrincipal
import com.snapiter.backend.security.DevicePrincipal
import com.snapiter.backend.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.net.URI

@RestController
@RequestMapping("/api/trackables")
@Tag(name = "Trackable", description = "Endpoints to do work with trackables")
@PreAuthorize("hasAnyRole('USER','DEVICE')")
@SecurityRequirement(name = "bearerAuth")
class TrackableController(
    private val trackableService: TrackableService
) {
    @PostMapping()
    @Operation(
        summary = "Create a new trackable",
        description = "Register a new trackable."
    )
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Bad request")
    fun create(
        @RequestBody req: CreateTrackableRequest,
        @AuthenticationPrincipal principal: AppPrincipal,
    ): Mono<ResponseEntity<Void>> {
        return when (principal) {
            is DevicePrincipal -> {
                trackableService.createTracker(req).map {
                    ResponseEntity.created(URI.create("/api/trackables/${it}")).build()
                }
            }

            is UserPrincipal -> {
                trackableService.createTracker(req).map {
                    ResponseEntity.created(URI.create("/api/trackables/${it}")).build()
                }
            }
        }
    }

    @GetMapping("/{trackableId}")
    @Operation(summary = "Get a trackable by trackable identifier")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Not found")
    fun getById(@PathVariable trackableId: String): Mono<ResponseEntity<Trackable>> =
        trackableService.getByTrackableId(trackableId)
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.notFound().build())

    @GetMapping("/host/{hostName}")
    @Operation(summary = "Get a trackable by host name")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Not found")
    fun getByHost(@PathVariable hostName: String): Mono<ResponseEntity<Trackable>> =
        trackableService.getByHostName(hostName)
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
}

data class CreateTrackableRequest(
    @field:NotBlank
    val name: String,
    val websiteTitle: String? = null,
    val website: String? = null,
    val hostName: String? = null,
    val icon: String? = null
)

