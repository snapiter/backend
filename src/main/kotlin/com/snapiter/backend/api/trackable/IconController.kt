package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.markers.Marker
import com.snapiter.backend.util.s3.FileResponseWrapperService
import com.snapiter.backend.util.s3.S3FileUpload
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.http.codec.multipart.Part
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.nio.channels.Channels

@RestController
@RequestMapping("/api/trackables/{trackableId}")
@Tag(name = "Icon", description = "Specific icon to show on the map for trackable")
class IconController(
    private val fileResponseWrapperService: FileResponseWrapperService,
    private val s3FileUpload: S3FileUpload
) {
    @GetMapping("/icon")
    fun thumbnailMarker(
        @PathVariable trackableId: String,
    ): ResponseEntity<Flux<ByteBuffer>> {
        return try {
            // normal case → SVG from S3
            fileResponseWrapperService.previewFile(
                trackableId,
                "icons/$trackableId/icon.svg",
                "image/svg+xml"
            )
        } catch (ex: Exception) {
            val resource = ClassPathResource("defaults/icon.gif")
            val inputStream = resource.inputStream
            val channel = Channels.newChannel(inputStream)

            val flux = Flux.generate<ByteBuffer> { sink ->
                val buffer = ByteBuffer.allocate(8192)
                val read = channel.read(buffer)
                if (read == -1) {
                    sink.complete()
                } else {
                    buffer.flip()
                    sink.next(buffer)
                }
            }.doFinally { channel.close() }

            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_GIF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"icon.gif\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(flux)
        }
    }

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
            s3FileUpload.saveFile("icon.svg", filePart, trackableId)
                .map {
                    ResponseEntity.ok("Icon uploaded successfully: ${it.filekey}")
                }
        }
    }


}