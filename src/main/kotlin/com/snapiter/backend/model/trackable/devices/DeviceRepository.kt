package com.snapiter.backend.model.trackable.devices

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface DeviceRepository : ReactiveCrudRepository<Device, Long> {
    fun findByDeviceIdAndTrackableId(deviceId: String, trackableId: String): Mono<Device>
}