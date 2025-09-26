package com.snapiter.backend.model.trackable.positionreport

import com.snapiter.backend.api.trackable.PositionRequest
import com.snapiter.backend.model.trackable.devices.Device
import com.snapiter.backend.model.trackable.devices.DeviceRepository
import com.snapiter.backend.model.trackable.trackable.TrackableRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.time.Instant
import com.snapiter.backend.model.trackable.trip.PositionType
import reactor.core.publisher.Flux

@Service
class PositionService(
    private val trackableRepository: TrackableRepository,
    private val deviceRepository: DeviceRepository,
    private val positionReportRepository: PositionReportRepository
) {
    fun report(trackableId: String, deviceId: String, positions: List<PositionRequest>): Flux<PositionReport> {
        return ensureDevice(trackableId, deviceId).flatMapMany { device ->
            val reports = positions.map { position ->
                val ts = position.createdAt ?: Instant.now()
                PositionReport.createFromLatAndLong(trackableId, position.latitude, position.longitude, ts)
            }

            // update device.lastReportedAt = last report's timestamp
            device.lastReportedAt = reports.maxOf { it.createdAt!! }

            deviceRepository.save(device)
                .thenMany(positionReportRepository.saveAll(reports))
        }
    }


    fun positions(
        positionType: PositionType,
        trackableId: String,
        fromDate: Instant?,
        untilDate: Instant?,
        page: Int,
        size: Int
    ): Flux<PositionReport> {
        return trackableRepository.findByTrackableId(trackableId).flatMapMany { trackable ->
            when (positionType) {
                PositionType.HOURLY -> {
                    if (fromDate != null && untilDate != null) {
                        positionReportRepository.findAllByTrackableIdAndCreatedAtIsBetweenOrderByCreatedAtDescAndTruncatedByHour(trackableId, fromDate, untilDate, page, size)
                    } else {
                        positionReportRepository.findAllByTrackableIdAndTruncateByHour(trackableId, page, size)
                    }
                }
                PositionType.ALL -> {
                    if (fromDate != null && untilDate != null) {
                        positionReportRepository.findAllByTrackableIdAndCreatedAtIsBetweenOrderByCreatedAtDesc(trackableId, fromDate, untilDate, page, size)
                    } else {
                        positionReportRepository.findAllByTrackableId(trackableId, page, size)
                    }
                }
            }
        }
    }

    private fun ensureDevice(trackableId: String, deviceId: String): Mono<Device> =
        deviceRepository.findByDeviceIdAndTrackableId(deviceId, trackableId)
            .switchIfEmpty(
                Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found for trackable"))
            )

}
