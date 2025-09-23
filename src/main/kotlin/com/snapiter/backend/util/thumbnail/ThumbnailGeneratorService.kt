package com.snapiter.backend.util.thumbnail

import com.snapiter.backend.configuration.S3ClientConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

@Service
@EnableConfigurationProperties(S3ClientConfigurationProperties::class)
class ThumbnailGeneratorService(
    private val s3client: S3AsyncClient,
    private val s3config: S3ClientConfigurationProperties,
    private val thumbnailGenerator: ThumbnailGenerator
) {
    fun create(fileId: String,dir: String, fileType: String, width: Number, height: Number) {
        try {
            putImage(
                fileId,
                dir,
                fileType,
                thumbnailGenerator.createThumbnail(
                    getS3ImageAsInputStream(
                        s3config.filesDir + dir + fileId
                    ),
                    fileType,
                    width,
                    height
                )
            )
        } catch (e: Throwable) {
            if(e is software.amazon.awssdk.services.s3.model.NoSuchKeyException) {
                throw e
            }
            throw CouldNotGenerateThumbnail(e.message?:"no message");
        }
    }

    fun getS3ImageAsInputStream(objectKey: String): InputStream {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(s3config.bucket)
            .key(objectKey)
            .build()

        val getObjectResponse = s3client.getObject(
            getObjectRequest,
            AsyncResponseTransformer.toBytes()
        ).get()

        return getObjectResponse.asInputStream()
    }


    private fun putImage(fileId: String, dir: String, fileType: String, image: BufferedImage) {
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", baos)
        val bytes = baos.toByteArray()
        val key = "thumbnails/$fileId"
        val objectRequest = PutObjectRequest.builder()
            .bucket(s3config.bucket)
            .key(s3config.filesDir + dir + key)
            .contentType(fileType)
            .build()

        val requestBody = AsyncRequestBody.fromBytes(bytes)
        s3client.putObject(
            objectRequest,
            requestBody
        ).get()
    }
}
