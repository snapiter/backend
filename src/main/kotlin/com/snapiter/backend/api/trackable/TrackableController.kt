package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.trackable.Trackable
import com.snapiter.backend.model.trackable.trackable.TrackableService
import com.snapiter.backend.security.AppPrincipal
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
@SecurityRequirement(name = "deviceToken")
class TrackableController(
    private val trackableService: TrackableService
) {
    @PostMapping()
    @Operation(
        summary = "Create a new trackable",
        description = "Register a new trackable."
    )
    @ApiResponse(responseCode = "200", description = "Created a new trackable")
    @ApiResponse(responseCode = "400", description = "Bad request")
    fun create(
        @RequestBody req: CreateTrackableRequest,
        @AuthenticationPrincipal principal: AppPrincipal,
    ): Mono<ResponseEntity<Trackable>> {
        return trackableService.createTracker(req, principal.userId).map {
            ResponseEntity.ok(it)
        }
    }


    @GetMapping("")
    @Operation(summary = "Get all trackables")
    @ApiResponse(responseCode = "200", description = "OK")
    fun getAll(@AuthenticationPrincipal principal: AppPrincipal): Mono<ResponseEntity<List<Trackable>>> =
        trackableService.findAllByUserId(principal.userId)
            .collectList()
            .map { trackables ->
                 ResponseEntity.ok(trackables)
            }


    @PreAuthorize("@trackableAccessChecker.canAccess(#trackableId, authentication)")
    @GetMapping("/{trackableId}")
    @Operation(summary = "Get a trackable by trackable identifier")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Not found")
    fun getById(@PathVariable trackableId: String): Mono<ResponseEntity<Trackable>> =
        trackableService.getByTrackableId(trackableId)
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.notFound().build())

}

data class CreateTrackableRequest(
    @field:NotBlank
    val name: String,
    val title: String? = null,
    val hostName: String? = null,
    val icon: String? = null
)

