package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.trackable.Trackable
import com.snapiter.backend.model.trackable.trackable.TrackableRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.net.URI
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/trackables")
@Tag(name = "Trackable", description = "XXXX")
class TrackableController(
    private val repository: TrackableRepository
) {
    @PostMapping()

     @Operation(
     summary = "Create a new trackable",
     description = "Register a new trackable in the system." +
                 "If the `trackableId` already exists, a 409 Conflict is returned."
         )
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Bad request")
    @ApiResponse(responseCode = "409", description = "Duplicate/conflict")
    fun create(@RequestBody req: CreateTrackableRequest): Mono<ResponseEntity<Void>> {
        val entity = Trackable(
            trackableId = UUID.randomUUID().toString(),
            name = req.name,
            websiteTitle = req.websiteTitle ?: "",
            website = req.website ?: "",
            hostName = req.hostName ?: "",
            icon = req.icon ?: "",
            createdAt = LocalDateTime.now()
        )

        return repository.save(entity).map { saved ->
            ResponseEntity.created(URI.create("/api/trackables/${saved.trackableId}")).build()
        }
    }
}

data class CreateTrackableRequest(
    @field:NotBlank
    val name: String,
    val websiteTitle: String? = null,
    val website: String? = null,
    val hostName: String? = null,
    val icon: String? = null
)
