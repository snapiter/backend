package com.snapiter.backend.api.trackable;

import com.snapiter.backend.model.trackable.positionreport.PositionService;
import com.snapiter.backend.util.staticmap.StaticMap
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@RestController
@RequestMapping("/api/trackables")
@Tag(name = "Trackable Positions", description = "Endpoint for devices to send their current geographic position.")
class StaticMapController (
    private val positionService:PositionService,
    private val staticMap: StaticMap
){
    @Operation(
        summary = "Get static map image for the last known position",
        description = "Generates and returns a static PNG map centered on the last known position of the specified trackable."
    )
    @GetMapping("/{trackableId}/positions/last/staticmap")
    @ApiResponse(responseCode = "404", description = "Could not find vesselId")
    fun getStaticMap(
        @PathVariable trackableId: String,
        @RequestParam("zoom", defaultValue = "10") zoom: Int,
        @RequestParam("size", defaultValue = "400") imageSize: Int
    ): Mono<ResponseEntity<ByteArray>> = mono {
        val position = positionService.lastPosition(trackableId).awaitSingleOrNull()
            ?: return@mono ResponseEntity.notFound().build()

        val image = staticMap.generateMapImage(trackableId,position.latitude, position.longitude, zoom, imageSize)
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "png", outputStream)
        val imageBytes = outputStream.toByteArray()

        val headers = HttpHeaders()
        headers.contentType = MediaType.IMAGE_PNG

        ResponseEntity(imageBytes, headers, HttpStatus.OK)
    }

}