package com.snapiter.backend.util.s3

import reactor.core.publisher.Flux
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.core.async.SdkPublisher
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

class FluxByteBufferResponseTransformer : AsyncResponseTransformer<GetObjectResponse, Flux<ByteBuffer>> {

    private val future = CompletableFuture<Flux<ByteBuffer>>()

    override fun prepare(): CompletableFuture<Flux<ByteBuffer>> = future

    override fun onResponse(sdkResponse: GetObjectResponse) {}

    override fun onStream(publisher: SdkPublisher<ByteBuffer>) {
        future.complete(Flux.from(publisher))
    }

    override fun exceptionOccurred(error: Throwable) {
        future.completeExceptionally(error)
    }
}
