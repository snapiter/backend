package com.snapiter.backend.model.trackable.positionreport

import com.snapiter.backend.api.trackable.PositionRequest
import com.snapiter.backend.model.trackable.devices.DeviceRepository
import com.snapiter.backend.model.trackable.trackable.TrackableRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.OffsetDateTime
import com.snapiter.backend.model.trackable.trip.PositionType
import reactor.core.publisher.Flux

@Service
class PositionService(
    private val trackableRepository: TrackableRepository,
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

    fun positions(
        positionType: PositionType,
        trackableId: String,
        fromDate: LocalDateTime?,
        untilDate: LocalDateTime?,
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

    private fun ensureDevice(trackableId: String, deviceId: String): Mono<Unit> =
        deviceRepository.findByDeviceIdAndTrackableId(deviceId, trackableId)
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found for trackable")))
            .thenReturn(Unit)
}
