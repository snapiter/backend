package com.snapiter.backend.api.trackable

import com.snapiter.backend.util.s3.FileResponseWrapperService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.nio.ByteBuffer
import java.nio.channels.Channels

@RestController
@RequestMapping("/api/trackables/{trackableId}")
@Tag(name = "Icon", description = "Specific icon to show on the map for trackable")
class PublicIconController(
    private val fileResponseWrapperService: FileResponseWrapperService
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
}