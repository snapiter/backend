package com.snapiter.backend.model.trackable.devices

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface DeviceRepository : ReactiveCrudRepository<Device, Long> {
    fun existsByTrackableIdAndDeviceId(trackableId: String, deviceId: String): Mono<Boolean>
    fun findByDeviceIdAndTrackableId(deviceId: String, trackableId: String): Mono<Device>
    fun findByTrackableId(trackableId: String): Flux<Device>


    @Query("""
        SELECT t.user_id
        FROM devices d
        JOIN trackables t ON d.trackable_id = t.trackable_id
        WHERE d.device_id = :deviceId
        LIMIT 1
    """)
    fun findUserIdByDeviceId(deviceId: String): Mono<UUID>
}