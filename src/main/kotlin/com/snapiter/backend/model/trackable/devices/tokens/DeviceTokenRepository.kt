package com.snapiter.backend.model.trackable.devices.tokens

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface DeviceTokenRepository : ReactiveCrudRepository<DeviceToken, Long> {
    fun findByTrackableId(trackableId: String): Mono<DeviceToken>
    fun findByTokenHash(tokenHash: String): Mono<DeviceToken>
    fun findByTrackableIdAndDeviceIdIsNull(trackableId: String): Mono<DeviceToken>
}
