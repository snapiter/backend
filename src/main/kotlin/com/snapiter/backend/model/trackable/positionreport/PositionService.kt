package com.snapiter.backend.model.trackable.positionreport

import com.snapiter.backend.api.trackable.PositionRequest
import com.snapiter.backend.model.trackable.devices.DeviceRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

@Service
class PositionService(
    private val deviceRepository: DeviceRepository,
    private val positionReportRepository: PositionReportRepository
) {
    fun report(trackableId: String, deviceId: String, position: PositionRequest): Mono<PositionReport> {
        return ensureDevice(trackableId, deviceId).flatMap {
            val ts = position.createdAt ?: OffsetDateTime.now()
            val pr = PositionReport.createFromLatAndLong(trackableId, position.latitude, position.longitude, ts)
            positionReportRepository.save(pr)
        }
    }


    private fun ensureDevice(trackableId: String, deviceId: String): Mono<Unit> =
        deviceRepository.findByDeviceIdAndTrackableId(deviceId, trackableId)
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found for trackable")))
            .thenReturn(Unit)
}
