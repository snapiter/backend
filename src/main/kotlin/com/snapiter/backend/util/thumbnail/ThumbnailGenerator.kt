package com.snapiter.backend.util.thumbnail

import net.coobird.thumbnailator.Thumbnails
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.InputStream

@Service
class ThumbnailGenerator {
    fun createThumbnail(inputStream: InputStream, fileType: String, width: Number, height: Number): BufferedImage {
        return Thumbnails.of(inputStream).size(width.toInt(), height.toInt()).asBufferedImage()
    }

}
