package com.snapiter.backend.util.s3

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.nio.ByteBuffer
import com.snapiter.backend.configuration.S3ClientConfigurationProperties


@Service
@EnableConfigurationProperties(S3ClientConfigurationProperties::class)
class S3FileDownload(
    private val s3client: S3AsyncClient,
    private val s3config: S3ClientConfigurationProperties
) {

    fun downloadFileAsFlux(fileId: String): Flux<ByteBuffer> {
        val request = GetObjectRequest.builder()
            .bucket(s3config.bucket)
            .key(s3config.filesDir + fileId)
            .build();

        return Mono.fromFuture(s3client.getObject(request, FluxByteBufferResponseTransformer()))
            .flatMapMany { it }
    }
}
