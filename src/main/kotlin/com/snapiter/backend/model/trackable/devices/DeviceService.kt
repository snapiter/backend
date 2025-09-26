package com.snapiter.backend.model.trackable.devices

import com.snapiter.backend.model.trackable.devices.tokens.DeviceToken
import com.snapiter.backend.model.trackable.devices.tokens.DeviceTokenService
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@Service
class DeviceService(
    private val deviceRepository: DeviceRepository,
    private val deviceTokenService: DeviceTokenService
) {
    fun createDevice(deviceToken: DeviceToken, deviceId: String, name: String): Mono<Device> {

        val now = Instant.now()
        val device = Device(
            id = null,
            trackableId = deviceToken.trackableId,
            deviceId = deviceId,
            createdAt = now,
            lastReportedAt = now,
            name = name
        )

        return deviceRepository.findByDeviceIdAndTrackableId(deviceId, deviceToken.trackableId)
            .flatMap { existing ->
                deviceRepository.delete(existing) // remove old if exists
            }
            .then(deviceTokenService.assignDeviceToToken(deviceToken, deviceId))
            .then(deviceRepository.save(device))
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
