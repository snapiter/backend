package com.snapiter.backend.model.trackable.positionreport

import com.snapiter.backend.api.trackable.PositionRequest
import com.snapiter.backend.model.trackable.devices.Device
import com.snapiter.backend.model.trackable.devices.DeviceNotFoundException
import com.snapiter.backend.model.trackable.devices.DeviceRepository
import com.snapiter.backend.model.trackable.trackable.TrackableRepository
import org.springframework.stereotype.Service
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
            if (positions.isEmpty()) {
                return@flatMapMany Flux.empty()
            }

            if (positions.any { it.latitude !in -90.0..90.0 || it.longitude !in -180.0..180.0 }) {
                return@flatMapMany Flux.error(
                    InvalidCoordinateException("latitude must be in [-90, 90] and longitude in [-180, 180]")
                )
            }

            val now = Instant.now()
            if (positions.any { it.createdAt?.isAfter(now) == true }) {
                return@flatMapMany Flux.error(
                    PositionInFutureException("createdAt must not be in the future")
                )
            }

            val reports = positions.map { position ->
                val ts = position.createdAt ?: now
                PositionReport.createFromLatAndLong(trackableId, position.latitude, position.longitude, ts)
            }

            // advance lastReportedAt only forward, so an out-of-order/old batch never rewinds "last seen"
            val batchMax = positions.maxOf { it.createdAt ?: now }
            device.lastReportedAt = maxOf(device.lastReportedAt ?: batchMax, batchMax)

            deviceRepository.save(device)
                .thenMany(positionReportRepository.saveAll(reports))
        }
    }

    fun lastPosition(trackableId: String): Mono<PositionReport> {
        return trackableRepository.findByTrackableId(trackableId)
            .flatMap {
                positionReportRepository.findFirstByTrackableIdOrderByCreatedAtDesc(trackableId)
            }
    }


    fun positions(
        positionType: PositionType,
        trackableId: String,
        fromDate: Instant,
        untilDate: Instant?,
        page: Int,
        size: Int
    ): Flux<PositionReport> {
        return trackableRepository.findByTrackableId(trackableId).flatMapMany { trackable ->
            val effectiveUntilDate = untilDate ?: Instant.now()

            when (positionType) {
                PositionType.HOURLY ->
                    positionReportRepository.findAllByTrackableIdAndCreatedAtIsBetweenOrderByCreatedAtDescAndTruncatedByHour(
                        trackableId,
                        fromDate,
                        effectiveUntilDate, 
                        page,
                        size
                    )

                PositionType.ALL ->
                    positionReportRepository.findAllByTrackableIdAndCreatedAtIsBetweenOrderByCreatedAtDesc(
                        trackableId,
                        fromDate,
                        effectiveUntilDate,
                        page,
                        size
                    )
            }
        }

    }

    private fun ensureDevice(trackableId: String, deviceId: String): Mono<Device> =
        deviceRepository.findByDeviceIdAndTrackableId(deviceId, trackableId)
            .switchIfEmpty(
                Mono.error(DeviceNotFoundException("Device not found for trackable"))
            )
}

class PositionInFutureException(msg: String) : RuntimeException(msg)

class InvalidCoordinateException(msg: String) : RuntimeException(msg)
