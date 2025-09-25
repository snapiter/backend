package com.snapiter.backend.util.s3

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.SdkResponse
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.CompletableFuture
import com.snapiter.backend.configuration.S3ClientConfigurationProperties
import reactor.util.retry.Retry
import java.time.Duration

@Service
@EnableConfigurationProperties(S3ClientConfigurationProperties::class)
class S3FileUpload(
    private val s3client: S3AsyncClient,
    private val s3config: S3ClientConfigurationProperties
) {
    fun getHeadObjectResponse(fileName: String, retries: Int = 3): Mono<HeadObjectResponse> {
        return Mono.fromFuture { headObjectResponse(fileName) }
            .retryWhen(Retry.backoff(retries.toLong(), Duration.ofMillis(200)))
    }

    private fun headObjectResponse(fileName: String): CompletableFuture<HeadObjectResponse> {
        return s3client.headObject(
            HeadObjectRequest.builder()
                .bucket(s3config.bucket)
                .key(s3config.filesDir +  fileName)
                .build()
        )
    }
    fun saveFile(fileName: UUID, part: FilePart, trackableId: String): Mono<UploadState> {
        // Gather metadata
        val metadata: MutableMap<String, String?> = HashMap()
        var filename = part.filename()

        metadata["filename"] = "$filename"
        metadata["trackableid"] = "$trackableId"
        var mt: MediaType? = part.headers().contentType
        if (mt == null) {
            mt = MediaType.APPLICATION_OCTET_STREAM
        }

        // Create multipart upload request
        val uploadRequest: CompletableFuture<CreateMultipartUploadResponse> = s3client
            .createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                    .contentType(mt.toString())
                    .key(s3config.filesDir  + fileName.toString())
                    .metadata(metadata)
                    .bucket(s3config.bucket)
                    .build()
            )

        val uploadState = UploadState(
            s3config.bucket,
            fileName.toString(),
            mt.toString()
        )

        return Mono
            .fromFuture(uploadRequest)
            .flatMapMany {
                checkResult(it)
                uploadState.uploadId = it.uploadId()
                part.content()
            }
            .bufferUntil{ buffer: DataBuffer ->
                uploadState.buffered += buffer.readableByteCount()
                uploadState.totalBytes += buffer.readableByteCount().toLong()
                if (uploadState.buffered >= s3config.multipartMinPartSize) {
                    uploadState.buffered = 0
                    true
                } else {
                    false
                }
            }
            .mapNotNull {
                concatBuffers(it)
            }
            .flatMap {
                uploadPart(uploadState, it)
            }
            .onBackpressureBuffer()
            .reduce(uploadState) { state: UploadState, completedPart: CompletedPart ->
                state.completedParts[completedPart.partNumber()] = completedPart
                state
            }
            .flatMap {
                completeUpload(it)
            }
            .map {
                checkResult(it)
                uploadState
            }
    }


    private fun concatBuffers(buffers: List<DataBuffer>): ByteBuffer {
        var partSize = 0
        for (b in buffers) {
            partSize += b.readableByteCount()
        }
        val partData: ByteBuffer = ByteBuffer.allocate(partSize)
        buffers.forEach { buffer -> partData.put(buffer.toByteBuffer()) }

        // Reset read pointer to first byte
        partData.rewind()
        return partData
    }

    /**
     * Upload a single file part to the requested bucket
     * @param uploadState
     * @param buffer
     * @return
     */
    private fun uploadPart(uploadState: UploadState, buffer: ByteBuffer): Mono<CompletedPart> {
        val partNumber = ++uploadState.partCounter
        val request: CompletableFuture<UploadPartResponse> = s3client.uploadPart(
            UploadPartRequest.builder()
                .bucket(uploadState.bucket)
                .key(s3config.filesDir + uploadState.filekey)
                .partNumber(partNumber)
                .uploadId(uploadState.uploadId)
                .contentLength(buffer.capacity().toLong())
                .build(),
            AsyncRequestBody.fromPublisher(Mono.just(buffer))
        )
        return Mono
            .fromFuture(request)
            .map {
                checkResult(it)
                CompletedPart.builder()
                    .eTag(it.eTag())
                    .partNumber(partNumber)
                    .build()
            }
    }

    private fun completeUpload(state: UploadState): Mono<CompleteMultipartUploadResponse> {
        val multipartUpload = CompletedMultipartUpload.builder()
            .parts(state.completedParts.values)
            .build()
        return Mono.fromFuture(
            s3client.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                    .bucket(state.bucket)
                    .uploadId(state.uploadId)
                    .multipartUpload(multipartUpload)
                    .key(s3config.filesDir + state.filekey)
                    .build()
            )
        )
    }



    /**
     * check result from an API call.
     * @param result Result from an API call
     */
    private fun checkResult(result: SdkResponse) {
        if (result.sdkHttpResponse() == null || !result.sdkHttpResponse().isSuccessful) {
            throw UploadFailedException(result)
        }
    }


    /**
     * Holds upload state during a multipart upload
     */
    internal class UploadState(val bucket: String, val filekey: String, val contentType: String) {
        var uploadId: String? = null
        var partCounter = 0
        var completedParts: MutableMap<Int, CompletedPart> = HashMap()
        var buffered = 0
        var totalBytes: Long = 0
    }

    class UploadFailedException(response: SdkResponse) : RuntimeException() {
        private var statusCode = 0
        private var statusText: Optional<String>? = null

        init {
            val httpResponse = response.sdkHttpResponse()
            if (httpResponse != null) {
                statusCode = httpResponse.statusCode()
                statusText = httpResponse.statusText()
            } else {
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value()
                statusText = Optional.of("UNKNOWN")
            }
        }

    }
}
