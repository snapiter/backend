package com.snapiter.backend.model.trackable.devices

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class DeviceServiceTest {
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var deviceService: DeviceService

    @BeforeEach
    fun setUp() {
        deviceRepository = Mockito.mock(DeviceRepository::class.java)
        deviceService = DeviceService(deviceRepository)
    }

    @Test
    fun `createDevice saves with correct fields and returns saved device`() {
        val trackableId = "track-123"
        val deviceId = "dev-abc"

        val captor: ArgumentCaptor<Device> = ArgumentCaptor.forClass(Device::class.java)

        Mockito.`when`(deviceRepository.save(captor.capture()))
            .thenAnswer { invocation ->
                val d = invocation.arguments[0] as Device
                // Simulate DB assigning a Long id
                Mono.just(d.copy(id = 1L))
            }

        val before = LocalDateTime.now().minusSeconds(2)

        val result = deviceService.createDevice(trackableId, deviceId)

        StepVerifier.create(result)
            .assertNext { saved ->
                assertEquals(1L, saved.id)
                assertEquals(trackableId, saved.trackableId)
                assertEquals(deviceId, saved.deviceId)

                val createdAt = saved.createdAt
                val lastReportedAt = saved.lastReportedAt
                assertNotNull(createdAt, "createdAt should not be null")
                assertNotNull(lastReportedAt, "lastReportedAt should not be null")
                assertEquals(createdAt, lastReportedAt, "createdAt and lastReportedAt should be equal")

                val after = LocalDateTime.now().plusSeconds(2)
                assertTrue(
                    createdAt.isAfter(before) && createdAt.isBefore(after),
                    "createdAt should be close to now, but was $createdAt"
                )
            }
            .verifyComplete()

        // Validate what was passed to repository.save
        val sent = captor.value
        assertEquals(null, sent.id)
        assertEquals(trackableId, sent.trackableId)
        assertEquals(deviceId, sent.deviceId)
        assertNotNull(sent.createdAt)
        assertEquals(sent.createdAt, sent.lastReportedAt)
    }

    @Test
    fun `getDevice returns repository result`() {
        val trackableId = "track-1"
        val deviceId = "dev-1"
        val device = Device(
            id = 42L,
            trackableId = trackableId,
            deviceId = deviceId,
            createdAt = LocalDateTime.now().minusDays(1),
            lastReportedAt = LocalDateTime.now()
        )

        Mockito.`when`(deviceRepository.findByDeviceIdAndTrackableId(deviceId, trackableId))
            .thenReturn(Mono.just(device))

        val result = deviceService.getDevice(trackableId, deviceId)

        StepVerifier.create(result)
            .expectNext(device)
            .verifyComplete()

        verify(deviceRepository).findByDeviceIdAndTrackableId(deviceId, trackableId)
    }

    @Test
    fun `deleteDevice returns true when device exists and is deleted`() {
        val trackableId = "track-2"
        val deviceId = "dev-2"
        val device = Device(
            id = 7L,
            trackableId = trackableId,
            deviceId = deviceId,
            createdAt = LocalDateTime.now().minusDays(2),
            lastReportedAt = LocalDateTime.now()
        )

        Mockito.`when`(deviceRepository.findByDeviceIdAndTrackableId(deviceId, trackableId))
            .thenReturn(Mono.just(device))
        Mockito.`when`(deviceRepository.delete(device))
            .thenReturn(Mono.empty())

        val result = deviceService.deleteDevice(trackableId, deviceId)

        StepVerifier.create(result)
            .expectNext(true)
            .verifyComplete()

        verify(deviceRepository).findByDeviceIdAndTrackableId(deviceId, trackableId)
        verify(deviceRepository).delete(device)
    }

    @Test
    fun `deleteDevice returns false when device does not exist`() {
        val trackableId = "track-3"
        val deviceId = "dev-3"

        Mockito.`when`(deviceRepository.findByDeviceIdAndTrackableId(deviceId, trackableId))
            .thenReturn(Mono.empty())

        val result = deviceService.deleteDevice(trackableId, deviceId)

        StepVerifier.create(result)
            .expectNext(false)
            .verifyComplete()

        verify(deviceRepository).findByDeviceIdAndTrackableId(deviceId, trackableId)
        // Ensure delete is never called
        verify(deviceRepository, never()).delete(Mockito.any(Device::class.java))
    }
}
