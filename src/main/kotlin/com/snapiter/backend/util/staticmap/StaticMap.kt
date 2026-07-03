package com.snapiter.backend.util.staticmap

import com.snapiter.backend.util.s3.S3FileDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.net.HttpURLConnection
import java.net.URL
import javax.imageio.ImageIO
import kotlin.math.*
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Service
class StaticMap (
    private val s3FileDownload: S3FileDownload
){
    @Value("\${staticmap.server}")
    private var tileServer: String = ""
    @Value("\${staticmap.tilesize}")
    private var tileSize: Int = 256

    fun svgBytesToBufferedImage(svgBytes: ByteArray): BufferedImage {
        val input = TranscoderInput(ByteArrayInputStream(svgBytes))
        val outputStream = ByteArrayOutputStream()

        val transcoder = PNGTranscoder()
        transcoder.transcode(input, org.apache.batik.transcoder.TranscoderOutput(outputStream))
        outputStream.flush()

        val pngBytes = outputStream.toByteArray()
        return ImageIO.read(ByteArrayInputStream(pngBytes))
    }
    suspend fun generateMapImage(trackableId: String, lat: Double, lon: Double, zoom: Int, imageSize: Int): BufferedImage {
        val latRad = Math.toRadians(lat)
        val n = 1 shl zoom
        val xTileExact = (lon + 180.0) / 360.0 * n
        val yTileExact = (1.0 - ln(tan(latRad) + 1 / cos(latRad)) / PI) / 2.0 * n

        val tilesNeeded = ceil(imageSize.toDouble() / tileSize).toInt() + 2
        val halfTilesNeeded = tilesNeeded / 2

        val image = BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB)
        val graphics: Graphics2D = image.createGraphics()

        val startX = floor(xTileExact - halfTilesNeeded).toInt()
        val startY = floor(yTileExact - halfTilesNeeded).toInt()

        val offsetX = ((xTileExact - startX) * tileSize - imageSize / 2).toInt()
        val offsetY = ((yTileExact - startY) * tileSize - imageSize / 2).toInt()

        withContext(Dispatchers.IO) {
            for (x in 0 until tilesNeeded) {
                for (y in 0 until tilesNeeded) {
                    val tileX = startX + x
                    val tileY = startY + y
                    val tileImage = fetchTileImage(zoom, tileX, tileY)
                    if (tileImage != null) {
                        graphics.drawImage(tileImage, x * tileSize - offsetX, y * tileSize - offsetY, null)
                    }
                }
            }
        }

        val markerImage = markerImage(trackableId)

        // Draw the marker image at the center
        val centerX = imageSize / 2
        val centerY = imageSize / 2
        val markerWidth = markerImage.width
        val markerHeight = markerImage.height
        val iconXOffset = 5
        val iconYOffset = -3
        graphics.drawImage(markerImage, (centerX - markerWidth / 2) + iconXOffset, (centerY - markerHeight / 2) + iconYOffset, null)

        graphics.dispose()
        return image
    }

    private fun fetchTileImage(zoom: Int, tileX: Int, tileY: Int): BufferedImage? {
        var connection: HttpURLConnection? = null
        return try {
            val tileURLString = tileServer
                .replace("{zoom}", zoom.toString())
                .replace("{x}", tileX.toString())
                .replace("{y}", tileY.toString())

            val tileURL = URL(tileURLString)
            connection = tileURL.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "SnapIter/1.0 (info@snapiter.com)")
            connection.setRequestProperty("Referer", "https://snapiter.com")
            connection.connect()
            if (connection.responseCode == 200) {
                ImageIO.read(connection.inputStream)
            } else {
                println("Failed to fetch tile: ${connection.responseCode} ${connection.responseMessage}")
                null
            }
        } catch (e: Exception) {
            println("Error fetching tile: ${e.message}")
            null
        } finally {
            connection?.disconnect()

        }
    }


    private suspend fun markerImage(trackableId: String): BufferedImage {
        return try {
            // Fetch SVG bytes from S3
            val svgBytes = s3FileDownload.downloadFileAsFlux("$trackableId/icon.svg", "icons/")
                .collectList()
                .map { buffers ->
                    buffers.fold(ByteArray(0)) { acc, buf ->
                        acc + ByteArray(buf.remaining()).apply { buf.get(this) }
                    }
                }
                .awaitSingle()

            // Convert SVG → BufferedImage
            svgBytesToBufferedImage(svgBytes)
        } catch (e: Exception) {
            println("Falling back to default marker: ${e.message ?: e.cause?.message ?: e}")
            val resource = ClassPathResource("defaults/icon.gif")
            resource.inputStream.use { ImageIO.read(it) }
        }
    }

}
