package com.snapiter.backend.model.trackable.devices.tokens

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface DeviceTokenRepository : ReactiveCrudRepository<DeviceTokenEntity, Long> {
    fun findByDeviceId(deviceId: String): Mono<DeviceTokenEntity>
    fun findByTokenHash(tokenHash: String): Mono<DeviceTokenEntity>
}
