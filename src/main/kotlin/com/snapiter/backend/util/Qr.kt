package com.snapiter.backend.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

object Qr {
    fun dataUrl(content: String, size: Int = 320): String {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until size) for (y in 0 until size) {
            val black = -0x1000000; val white = -0x1
            image.setRGB(x, y, if (matrix.get(x, y)) black else white)
        }
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        val b64 = Base64.getEncoder().encodeToString(baos.toByteArray())
        return "data:image/png;base64,$b64"
    }
}