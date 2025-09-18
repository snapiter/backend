package com.snapiter.backend.util.s3

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
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

    fun downloadFileFromKml(fileId: String): Mono<ResponseInputStream<GetObjectResponse>> {
        val request = GetObjectRequest.builder()
            .bucket(s3config.bucket)
            .key(s3config.filesDir + "kml/" + fileId)
            .build();

//        return Mono.fromFuture(s3client.getObject(request,  AsyncResponseTransformer.toBytes()))
        return Mono.fromFuture(s3client.getObject(request, AsyncResponseTransformer.toBlockingInputStream()))
//        return Mono.fromFuture(s3client.getObject(request, AsyncResponseTransformer.toBlockingInputStream()))
//            .map { inputStream ->
//                val reader = BufferedReader(InputStreamReader(inputStream))
//
//            var line: String?
//            while (reader.readLine().also { line = it } != null) {
//                println(line)
//            }
//            return "test";
//
//        }

//        return Mono.fromFuture(s3client.getObject(request, FluxByteBufferResponseTransformer()))
//            .flatMapMany { it }
    }
}
