package com.snapiter.backend.model.trackable.positionreport

import com.snapiter.backend.api.trackable.PositionRequest
import com.snapiter.backend.model.trackable.devices.Device
import com.snapiter.backend.model.trackable.devices.DeviceNotFoundException
import com.snapiter.backend.model.trackable.devices.DeviceRepository
import com.snapiter.backend.model.trackable.trackable.TrackableRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class PositionServiceTest {
    private lateinit var trackableRepository: TrackableRepository
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var positionReportRepository: PositionReportRepository
    private lateinit var positionService: PositionService

    private val trackableId = "track-1"
    private val deviceId = "dev-1"

    @BeforeEach
    fun setUp() {
        trackableRepository = Mockito.mock(TrackableRepository::class.java)
        deviceRepository = Mockito.mock(DeviceRepository::class.java)
        positionReportRepository = Mockito.mock(PositionReportRepository::class.java)
        positionService = PositionService(trackableRepository, deviceRepository, positionReportRepository)
    }

    private fun device(lastReportedAt: Instant = Instant.now().minus(Duration.ofDays(1))) = Device(
        id = 1L,
        trackableId = trackableId,
        deviceId = deviceId,
        name = "NAME",
        createdAt = Instant.now().minus(Duration.ofDays(1)),
        lastReportedAt = lastReportedAt
    )

    @Test
    fun `should not save position reports on empty list`() {
        Mockito.`when`(deviceRepository.findByDeviceIdAndTrackableId(eq(deviceId), eq(trackableId)))
            .thenReturn(Mono.just(device()))

        val result = positionService.report(trackableId, deviceId, emptyList())

        StepVerifier.create(result).verifyComplete()

        verify(deviceRepository, never()).save(any())
        verify(positionReportRepository, never()).saveAll(any<Iterable<PositionReport>>())
    }

    @Test
    fun `should save position reports and update last reported`() {
        val earlier = Instant.parse("2025-01-01T10:00:00Z")
        val later = Instant.parse("2025-01-01T11:00:00Z")

        val captor: ArgumentCaptor<Device> = ArgumentCaptor.forClass(Device::class.java)
        Mockito.`when`(deviceRepository.findByDeviceIdAndTrackableId(eq(deviceId), eq(trackableId)))
            .thenReturn(Mono.just(device(lastReportedAt = Instant.parse("2024-01-01T00:00:00Z"))))
        Mockito.`when`(deviceRepository.save(captor.capture()))
            .thenAnswer { Mono.just(it.arguments[0] as Device) }
        Mockito.`when`(positionReportRepository.saveAll(any<Iterable<PositionReport>>()))
            .thenAnswer { Flux.fromIterable(it.arguments[0] as Iterable<PositionReport>) }

        val positions = listOf(
            PositionRequest(latitude = 1.0, longitude = 2.0, createdAt = later),
            PositionRequest(latitude = 3.0, longitude = 4.0, createdAt = earlier)
        )

        val result = positionService.report(trackableId, deviceId, positions)

        StepVerifier.create(result)
            .expectNextCount(2)
            .verifyComplete()

        assertEquals(later, captor.value.lastReportedAt)
    }

    @Test
    fun `should not update last reported if the date is before an earlier last reported`() {
        val existingLastReported = Instant.parse("2025-06-01T00:00:00Z")

        val captor: ArgumentCaptor<Device> = ArgumentCaptor.forClass(Device::class.java)
        Mockito.`when`(deviceRepository.findByDeviceIdAndTrackableId(eq(deviceId), eq(trackableId)))
            .thenReturn(Mono.just(device(lastReportedAt = existingLastReported)))
        Mockito.`when`(deviceRepository.save(captor.capture()))
            .thenAnswer { Mono.just(it.arguments[0] as Device) }
        Mockito.`when`(positionReportRepository.saveAll(any<Iterable<PositionReport>>()))
            .thenAnswer { Flux.fromIterable(it.arguments[0] as Iterable<PositionReport>) }

        // batch is older than the device's current lastReportedAt
        val positions = listOf(
            PositionRequest(latitude = 1.0, longitude = 2.0, createdAt = Instant.parse("2025-01-01T10:00:00Z"))
        )

        val result = positionService.report(trackableId, deviceId, positions)

        StepVerifier.create(result)
            .expectNextCount(1)
            .verifyComplete()

        assertEquals(existingLastReported, captor.value.lastReportedAt)
    }

    @Test
    fun `should reject a position in the future`() {
        Mockito.`when`(deviceRepository.findByDeviceIdAndTrackableId(eq(deviceId), eq(trackableId)))
            .thenReturn(Mono.just(device()))

        val future = Instant.now().plus(Duration.ofDays(1))
        val result = positionService.report(
            trackableId,
            deviceId,
            listOf(PositionRequest(latitude = 1.0, longitude = 2.0, createdAt = future))
        )

        StepVerifier.create(result)
            .expectError(PositionInFutureException::class.java)
            .verify()

        verify(deviceRepository, never()).save(any())
        verify(positionReportRepository, never()).saveAll(any<Iterable<PositionReport>>())
    }

    @Test
    fun `should reject a position when coordinates are out of bounds`() {
        Mockito.`when`(deviceRepository.findByDeviceIdAndTrackableId(eq(deviceId), eq(trackableId)))
            .thenReturn(Mono.just(device()))

        val result = positionService.report(
            trackableId,
            deviceId,
            listOf(PositionRequest(latitude = 99.0, longitude = 2.0, createdAt = Instant.parse("2025-01-01T10:00:00Z")))
        )

        StepVerifier.create(result)
            .expectError(InvalidCoordinateException::class.java)
            .verify()

        verify(deviceRepository, never()).save(any())
        verify(positionReportRepository, never()).saveAll(any<Iterable<PositionReport>>())
    }

    @Test
    fun `should throw device not found`() {
        Mockito.`when`(deviceRepository.findByDeviceIdAndTrackableId(eq(deviceId), eq(trackableId)))
            .thenReturn(Mono.empty())

        val result = positionService.report(
            trackableId,
            deviceId,
            listOf(PositionRequest(latitude = 1.0, longitude = 2.0, createdAt = Instant.now()))
        )

        StepVerifier.create(result)
            .expectError(DeviceNotFoundException::class.java)
            .verify()

        verify(deviceRepository, never()).save(any())
        verify(positionReportRepository, never()).saveAll(any<Iterable<PositionReport>>())
    }
}
