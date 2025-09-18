package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.trackable.Trackable
import com.snapiter.backend.model.trackable.trackable.TrackableService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/trackables")
@Tag(name = "Trackable", description = "Endpoints to do work with trackables")
class PublicTrackableController(
    private val trackableService: TrackableService
) {
    @GetMapping("/host/{hostName}")
    @Operation(
        summary = "Get a trackable by host name",
        security = []
    )
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Not found")
    fun getByHost(@PathVariable hostName: String): Mono<ResponseEntity<Trackable>> =
        trackableService.getByHostName(hostName)
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
}