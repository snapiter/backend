package com.snapiter.backend.api.trackable

import com.snapiter.backend.util.s3.FileResponseWrapperService
import com.snapiter.backend.util.s3.S3FileUpload
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/trackables/{trackableId}")
@Tag(name = "Icon", description = "Specific icon to show on the map for trackable")
@PreAuthorize("hasAnyRole('USER', 'DEVICE')")
@SecurityRequirement(name = "deviceToken")
@SecurityRequirement(name = "bearerAuth")
class IconController(
    private val s3FileUpload: S3FileUpload
) {
    @PostMapping("/icon", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Upload the icon",
        description = "Upload the icon image (must be SVG)."
    )
    fun uploadImage(
        @PathVariable trackableId: String,
        parts: Flux<Part>
    ): Mono<ResponseEntity<String>> {
        return parts.collectList().flatMap { allParts ->
            val fileParts = allParts.filterIsInstance<FilePart>()

            if (fileParts.isEmpty()) {
                return@flatMap Mono.just(ResponseEntity.badRequest().body("No file provided"))
            }

            val filePart = fileParts.first()

            // Enforce SVG by content type
            val contentType = filePart.headers().contentType
            if (contentType == null || contentType.toString() != "image/svg+xml") {
                return@flatMap Mono.just(ResponseEntity.badRequest().body("Only SVG icons are allowed"))
            }

            // Enforce .svg extension
            if (!filePart.filename().lowercase().endsWith(".svg")) {
                return@flatMap Mono.just(ResponseEntity.badRequest().body("Filename must end with .svg"))
            }

            // Save as fixed "icon.svg"
            s3FileUpload.saveFile("icon.svg", filePart, trackableId, "icons/")
                .map {
                    ResponseEntity.ok("Icon uploaded successfully: ${it.filekey}")
                }
        }
    }


}