package com.snapiter.backend.model.trackable.devices

import com.snapiter.backend.model.trackable.trackable.Trackable
import com.snapiter.backend.model.trackable.trackable.TrackableService
import com.snapiter.backend.model.users.User
import com.snapiter.backend.model.users.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class DeviceAlertServiceTest {
    private lateinit var trackableService: TrackableService
    private lateinit var deviceService: DeviceService
    private lateinit var userRepository: UserRepository
    private lateinit var mailSender: JavaMailSender
    private lateinit var deviceAlertService: DeviceAlertService

    private val fromEmail = "noreply@snapiter.com"

    @BeforeEach
    fun setUp() {
        trackableService = Mockito.mock(TrackableService::class.java)
        deviceService = Mockito.mock(DeviceService::class.java)
        userRepository = Mockito.mock(UserRepository::class.java)
        mailSender = Mockito.mock(JavaMailSender::class.java)
        deviceAlertService = DeviceAlertService(trackableService, deviceService, userRepository, mailSender, fromEmail)
    }

    @Test
    fun `alert emails the trackable owner with the device name`() {
        val trackableId = "track-123"
        val deviceId = "dev-abc"
        val userId = UUID.randomUUID()

        Mockito.`when`(trackableService.getByTrackableId(trackableId))
            .thenReturn(Mono.just(trackable(trackableId, userId)))
        Mockito.`when`(userRepository.findByUserId(userId))
            .thenReturn(Mono.just(user(userId, "owner@example.com")))
        Mockito.`when`(deviceService.getDevice(trackableId, deviceId))
            .thenReturn(Mono.just(device(trackableId, deviceId, name = "Front door tracker")))

        val captor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)

        StepVerifier.create(deviceAlertService.alert(trackableId, deviceId))
            .verifyComplete()

        verify(mailSender).send(captor.capture())
        val sent = captor.value
        assertEquals(fromEmail, sent.from)
        assertEquals(listOf("owner@example.com"), sent.to?.toList())
        assertNotNull(sent.text)
        assertTrue(
            sent.text!!.contains("Front door tracker"),
            "email body should mention the device name, but was: ${sent.text}"
        )
    }

    @Test
    fun `alert falls back to the deviceId when the device has no name`() {
        val trackableId = "track-123"
        val deviceId = "dev-abc"
        val userId = UUID.randomUUID()

        Mockito.`when`(trackableService.getByTrackableId(trackableId))
            .thenReturn(Mono.just(trackable(trackableId, userId)))
        Mockito.`when`(userRepository.findByUserId(userId))
            .thenReturn(Mono.just(user(userId, "owner@example.com")))
        Mockito.`when`(deviceService.getDevice(trackableId, deviceId))
            .thenReturn(Mono.just(device(trackableId, deviceId, name = null)))

        val captor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)

        StepVerifier.create(deviceAlertService.alert(trackableId, deviceId))
            .verifyComplete()

        verify(mailSender).send(captor.capture())
        assertTrue(
            captor.value.text!!.contains(deviceId),
            "email body should fall back to the deviceId, but was: ${captor.value.text}"
        )
    }

    @Test
    fun `alert sends nothing when the trackable is not found`() {
        val trackableId = "missing"
        val deviceId = "dev-abc"

        Mockito.`when`(trackableService.getByTrackableId(trackableId))
            .thenReturn(Mono.empty())
        Mockito.`when`(deviceService.getDevice(trackableId, deviceId))
            .thenReturn(Mono.just(device(trackableId, deviceId, name = "Front door tracker")))

        StepVerifier.create(deviceAlertService.alert(trackableId, deviceId))
            .verifyComplete()

        verify(mailSender, never()).send(Mockito.any(SimpleMailMessage::class.java))
    }

    private fun trackable(trackableId: String, userId: UUID) = Trackable(
        id = 1L,
        trackableId = trackableId,
        name = "name",
        createdAt = Instant.now(),
        userId = userId
    )

    private fun user(userId: UUID, email: String) = User(
        id = 1L,
        userId = userId,
        email = email,
        emailVerified = true,
        displayName = null,
        createdAt = OffsetDateTime.now(ZoneOffset.UTC),
        lastLoginAt = null
    )

    private fun device(trackableId: String, deviceId: String, name: String?) = Device(
        id = 1L,
        trackableId = trackableId,
        deviceId = deviceId,
        name = name,
        createdAt = Instant.now(),
        lastReportedAt = Instant.now()
    )
}
