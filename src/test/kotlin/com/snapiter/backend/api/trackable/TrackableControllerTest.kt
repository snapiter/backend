package com.snapiter.backend.api.trackable

import com.snapiter.backend.TestAuthUtils.withDevicePrincipal
import com.snapiter.backend.TestSecurityConfig
import com.snapiter.backend.model.trackable.devices.DeviceRepository
import com.snapiter.backend.model.trackable.trackable.Trackable
import com.snapiter.backend.model.trackable.trackable.TrackableRepository
import com.snapiter.backend.model.trackable.trackable.TrackableService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import com.snapiter.backend.security.TrackableSecurityService
import org.mockito.kotlin.eq
import java.util.UUID

@WebFluxTest(controllers = [TrackableController::class])
@Import(TestSecurityConfig::class, TrackableSecurityService::class)
@AutoConfigureWebTestClient
class TrackableControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var service: TrackableService

    @MockitoBean
    lateinit var trackableRepository: TrackableRepository

    @MockitoBean
    lateinit var deviceRepository: DeviceRepository

    @Test
    fun `POST create returns 201 and saves entity with createdAt`() {
        val captor = argumentCaptor<CreateTrackableRequest>()
        whenever(service.createTracker(any(),any())).thenReturn(Mono.just("trackableId"))

        val body = """
            {
              "name": "SnapIter name",
              "title": "SnapIter",
              "hostName": "snapiter.eu",
              "icon": "📍"
            }
        """.trimIndent()

        webTestClient
            .withDevicePrincipal()
            .post()
            .uri("/api/trackables")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isCreated
            .expectHeader().valueMatches("Location", "/api/trackables/.*")

        // Assert (repo interaction & saved entity)
        verify(service, times(1)).createTracker(captor.capture(), any())
        val saved = captor.firstValue

        assertEquals("SnapIter name", saved.name)
        assertEquals("SnapIter", saved.title)
        assertEquals("snapiter.eu", saved.hostName)
        assertEquals("📍", saved.icon)
    }

    @Test
    fun `GET by id returns 200 with entity`() {
        val entity = trackable()
        whenever(service.getByTrackableId("abc")).thenReturn(Mono.just(entity))

        whenever(deviceRepository.existsByTrackableIdAndDeviceId(eq("abc"), any()))
            .thenReturn(Mono.just(true))

        webTestClient
            .withDevicePrincipal()
            .get()
            .uri("/api/trackables/abc")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.trackableId").isEqualTo("abc")
            .jsonPath("$.name").isEqualTo("SnapIter name")
            .jsonPath("$.title").isEqualTo("SnapIter")
            .jsonPath("$.hostName").isEqualTo("snapiter.eu")
            .jsonPath("$.icon").isEqualTo("📍")
    }

    private fun trackable(): Trackable = Trackable(
        trackableId = "abc",
        name = "SnapIter name",
        title = "SnapIter",
        hostName = "snapiter.eu",
        icon = "📍",
        createdAt = LocalDateTime.parse("2025-09-10T12:34:56"),
        userId = UUID.randomUUID()
    )

    @Test
    fun `GET by id returns 404 when missing`() {
        whenever(service.getByTrackableId("missing")).thenReturn(Mono.empty())

        whenever(trackableRepository.findByTrackableId("missing"))
            .thenReturn(Mono.just(trackable()))


        whenever(deviceRepository.existsByTrackableIdAndDeviceId(eq("missing"), any()))
            .thenReturn(Mono.just(true))

        webTestClient
            .withDevicePrincipal()
            .get()
            .uri("/api/trackables/missing")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `GET by host returns 200 with entity`() {
        val entity = Trackable(
            trackableId = "xyz",
            name = "By Host",
            title = "SnapIter",
            hostName = "snapiter.eu",
            icon = "📍",
            createdAt = LocalDateTime.parse("2025-09-10T12:34:56"),
            userId = UUID.randomUUID()

        )


        whenever(service.getByHostName("snapiter.eu")).thenReturn(Mono.just(entity))

        webTestClient
            .withDevicePrincipal()
            .get()
            .uri("/api/trackables/host/snapiter.eu")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.trackableId").isEqualTo("xyz")
            .jsonPath("$.hostName").isEqualTo("snapiter.eu")
    }

    @Test
    fun `GET by host returns 404 when missing`() {
        whenever(service.getByHostName("nope.example")).thenReturn(Mono.empty())

        whenever(trackableRepository.findByTrackableId("abc"))
            .thenReturn(Mono.empty())
        webTestClient
            .withDevicePrincipal()
            .get()
            .uri("/api/trackables/host/nope.example")
            .exchange()
            .expectStatus().isNotFound

    }
}
