package com.snapiter.backend.model.trackable.devices

import com.snapiter.backend.model.trackable.devices.tokens.DeviceTokenService
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class DeviceService(
    private val deviceRepository: DeviceRepository,
    private val deviceTokenService: DeviceTokenService
) {
    fun issueDevice(trackableId: String, deviceId: String): Mono<String> {
        return this.createDevice(trackableId, deviceId).flatMap {
            deviceTokenService.issue(it.deviceId)
        }
    }

    fun createDevice(trackableId: String, deviceId: String): Mono<Device> {
        val now = LocalDateTime.now()
        val device = Device(
            id = null,
            trackableId = trackableId,
            deviceId = deviceId,
            createdAt = now,
            lastReportedAt = now
        )
        return deviceRepository.save(device)
    }

    fun getDevice(trackableId: String, deviceId: String): Mono<Device> {
        return deviceRepository.findByDeviceIdAndTrackableId(deviceId, trackableId)
    }

    fun getDevices(trackableId: String): Flux<Device> {
        return deviceRepository.findByTrackableId(trackableId)
    }


    /**
     * Deletes the device for (trackableId, deviceId).
     * @return Mono<Boolean> -> true if a device was found and deleted, false if nothing matched.
     */
    fun deleteDevice(trackableId: String, deviceId: String): Mono<Boolean> {
        return deviceRepository.findByDeviceIdAndTrackableId(deviceId, trackableId)
            .flatMap { deviceRepository.delete(it).thenReturn(true) }
            .defaultIfEmpty(false)
    }
}
