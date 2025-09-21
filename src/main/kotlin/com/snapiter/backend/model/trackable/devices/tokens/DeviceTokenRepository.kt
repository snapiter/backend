package com.snapiter.backend.model.trackable.devices.tokens

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface DeviceTokenRepository : ReactiveCrudRepository<DeviceToken, Long> {
    fun findByTrackableIdAndDeviceIdNotNull(trackableId: String): Flux<DeviceToken>
    fun findByTokenHash(tokenHash: String): Mono<DeviceToken>
}
