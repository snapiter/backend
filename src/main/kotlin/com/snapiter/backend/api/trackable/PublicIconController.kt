package com.snapiter.backend.api.trackable

import com.snapiter.backend.util.s3.S3FileDownload
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
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.nio.channels.Channels

@RestController
@RequestMapping("/api/trackables/{trackableId}")
@Tag(name = "Icon", description = "Specific icon to show on the map for trackable")
class PublicIconController(
    private val s3FileDownload: S3FileDownload
){
    @GetMapping("/icon")
    fun getIcon(
        @PathVariable trackableId: String,
    ): Mono<ResponseEntity<Flux<ByteBuffer>>> {
        return s3FileDownload.downloadFileAsFlux("$trackableId/icon.svg", "icons/")
            .collectList() // force evaluation once
            .flatMap { buffers ->
                val flux = Flux.fromIterable(buffers)
                val svgResponse = ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/svg+xml")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"icon.svg\"")
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400, immutable")
                    .body(flux)
                Mono.just(svgResponse)
            }
            .onErrorResume { ex ->
                println("Falling back: $ex")

                val resource = ClassPathResource("defaults/icon.gif")
                val inputStream = resource.inputStream
                val channel = Channels.newChannel(inputStream)

                val gifFlux = Flux.generate<ByteBuffer> { sink ->
                    val buffer = ByteBuffer.allocate(8192)
                    val read = channel.read(buffer)
                    if (read == -1) {
                        sink.complete()
                    } else {
                        buffer.flip()
                        sink.next(buffer)
                    }
                }.doFinally { channel.close() }

                val gifResponse = ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_GIF_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"icon.gif\"")
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                    .body(gifFlux)

                Mono.just(gifResponse)
            }
    }

}