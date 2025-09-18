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

@Service
@EnableConfigurationProperties(S3ClientConfigurationProperties::class)
class S3FileUpload(
    private val s3client: S3AsyncClient,
    private val s3config: S3ClientConfigurationProperties
) {
    fun getHeadObjectResponse(fileName: String, dir: String): HeadObjectResponse {
        return s3client.headObject(
            HeadObjectRequest.builder()
                .bucket(s3config.bucket)
                .key(s3config.filesDir + dir + fileName)
                .build()
        ).get()
    }
    fun saveFile(fileName: UUID, dir: String, part: FilePart, trackableId: String): Mono<String> {
        // Gather metadata
        val metadata: MutableMap<String, String?> = HashMap()
        var filename = part.filename()

        metadata["filename"] = "$filename"
        metadata["vesselid"] = "$trackableId"
        var mt: MediaType? = part.headers().contentType
        if (mt == null) {
            mt = MediaType.APPLICATION_OCTET_STREAM
        }

        // Create multipart upload request
        val uploadRequest: CompletableFuture<CreateMultipartUploadResponse> = s3client
            .createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                    .contentType(mt.toString())
                    .key(s3config.filesDir + dir + fileName.toString())
                    .metadata(metadata)
                    .bucket(s3config.bucket)
                    .build()
            )

        // This variable will hold the upload state that we must keep
        // around until all uploads complete
        val uploadState = UploadState(s3config.bucket, fileName.toString())
        return Mono
            .fromFuture(uploadRequest)
            .flatMapMany {
                checkResult(it)
                uploadState.uploadId = it.uploadId()
                part.content()
            }
            .bufferUntil{ buffer: DataBuffer ->
                uploadState.buffered += buffer.readableByteCount()
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
                uploadPart(uploadState, dir, it)
            }
            .onBackpressureBuffer()
            .reduce(uploadState) { state: UploadState, completedPart: CompletedPart ->
                state.completedParts[completedPart.partNumber()] = completedPart
                state
            }
            .flatMap {
                completeUpload(it, dir)
            }
            .map {
                checkResult(it)
                uploadState.filekey
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
    private fun uploadPart(uploadState: UploadState, dir: String, buffer: ByteBuffer): Mono<CompletedPart> {
        val partNumber = ++uploadState.partCounter
        val request: CompletableFuture<UploadPartResponse> = s3client.uploadPart(
            UploadPartRequest.builder()
                .bucket(uploadState.bucket)
                .key(s3config.filesDir + dir + uploadState.filekey)
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

    private fun completeUpload(state: UploadState, dir: String): Mono<CompleteMultipartUploadResponse> {
        val multipartUpload = CompletedMultipartUpload.builder()
            .parts(state.completedParts.values)
            .build()
        return Mono.fromFuture(
            s3client.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                    .bucket(state.bucket)
                    .uploadId(state.uploadId)
                    .multipartUpload(multipartUpload)
                    .key(s3config.filesDir + dir + state.filekey)
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
    internal class UploadState(val bucket: String, val filekey: String) {
        var uploadId: String? = null
        var partCounter = 0
        var completedParts: MutableMap<Int, CompletedPart> = HashMap()
        var buffered = 0
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
