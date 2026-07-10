package com.snapiter.backend.api.trackable

import com.snapiter.backend.TestAuthUtils.withUserPrincipal
import com.snapiter.backend.TestSecurityConfig
import com.snapiter.backend.model.trackable.devices.DeviceAlertService
import com.snapiter.backend.model.trackable.devices.DeviceRepository
import com.snapiter.backend.model.trackable.trackable.TrackableRepository
import com.snapiter.backend.security.TrackableSecurityService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.util.UUID

@WebFluxTest(controllers = [DeviceAlertController::class])
@Import(TestSecurityConfig::class, TrackableSecurityService::class)
class DeviceAlertControllerTest {

    @Autowired
    lateinit var context: ApplicationContext
    lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context)
            .apply(springSecurity())
            .configureClient()
            .build()
    }

    @MockitoBean
    lateinit var deviceAlertService: DeviceAlertService

    @MockitoBean
    lateinit var trackableRepository: TrackableRepository

    @MockitoBean
    lateinit var deviceRepository: DeviceRepository


    private val trackableId = "track-123"
    private val deviceId = "dev-abc"

    @Test
    fun `POST battery alert returns 202 and calls the service`() {
        val userId = UUID.randomUUID()
        whenever(trackableRepository.existsByTrackableIdAndUserId(eq(trackableId), eq(userId)))
            .thenReturn(Mono.just(true))
        whenever(deviceAlertService.alert(trackableId, deviceId)).thenReturn(Mono.empty())

        webTestClient
            .withUserPrincipal(userId = userId)
            .post()
            .uri("/api/trackables/$trackableId/devices/$deviceId/alert/battery")
            .exchange()
            .expectStatus().isAccepted

        verify(deviceAlertService, times(1)).alert(trackableId, deviceId)
    }

    @Test
    fun `POST battery alert returns 403 when the user cannot access the trackable`() {
        val userId = UUID.randomUUID()
        whenever(trackableRepository.existsByTrackableIdAndUserId(eq(trackableId), eq(userId)))
            .thenReturn(Mono.just(false))

        webTestClient
            .withUserPrincipal(userId = userId)
            .post()
            .uri("/api/trackables/$trackableId/devices/$deviceId/alert/battery")
            .exchange()
            .expectStatus().isForbidden

        verify(deviceAlertService, never()).alert(any(), any())
    }
}
