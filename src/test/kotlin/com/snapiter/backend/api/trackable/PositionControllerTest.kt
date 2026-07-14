package com.snapiter.backend.api.trackable

import com.snapiter.backend.TestAuthUtils.withDevicePrincipal
import com.snapiter.backend.TestSecurityConfig
import com.snapiter.backend.model.trackable.devices.DeviceNotFoundException
import com.snapiter.backend.model.trackable.positionreport.InvalidCoordinateException
import com.snapiter.backend.model.trackable.positionreport.PositionInFutureException
import com.snapiter.backend.model.trackable.positionreport.PositionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.server.ResponseStatusException
import org.junit.jupiter.api.BeforeEach
import reactor.core.publisher.Flux
import java.time.Instant
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest

@WebFluxTest(controllers = [PositionController::class])
@Import(TestSecurityConfig::class)
class PositionControllerTest {
    @Autowired lateinit var context: ApplicationContext
    lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context)
            .apply(springSecurity())
            .configureClient()
            .build()
    }

    @MockitoBean
    lateinit var positionService: PositionService


    @Test
    fun `should create a single position`() {
        val trackableId = "t-123"
        val deviceId = "d-456"
        val clientTs = Instant.parse("2025-09-12T09:00:00Z")

        val reqCaptor = argumentCaptor<List<PositionRequest>>()

        whenever(positionService.report(eq(trackableId), eq(deviceId), reqCaptor.capture()))
            .thenReturn(Flux.empty())

        val body = """
          { "latitude": 52.3702, "longitude": 4.8952, "createdAt": "$clientTs" }
        """.trimIndent()

        webTestClient
            .withDevicePrincipal()
            .post()
            .uri("/api/trackables/$trackableId/$deviceId/position")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isNoContent

        assertEquals(clientTs, reqCaptor.firstValue[0].createdAt)
    }

    @Test
    fun `should create multiple positions`() {
        val trackableId = "t-123"
        val deviceId = "d-456"

        whenever(positionService.report(eq(trackableId), eq(deviceId), any<List<PositionRequest>>()))
            .thenReturn(Flux.empty())

        val body = """
          [{ "latitude": 52.37, "longitude": 4.90, "createdAt": "2025-09-12T09:00:00Z" }]
        """.trimIndent()

        webTestClient
            .withDevicePrincipal()
            .post()
            .uri("/api/trackables/$trackableId/$deviceId/positions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isNoContent

        verify(positionService, times(1)).report(eq(trackableId), eq(deviceId), any())
        verifyNoMoreInteractions(positionService)
    }

    @Test
    fun `should fail single position when device not found`() {
        val trackableId = "trackableId"
        val deviceId = "deviceId"

        whenever(positionService.report(eq(trackableId), eq(deviceId), any()))
            .thenReturn(Flux.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found for trackable")))

        val body = """{ "latitude": 52.37, "longitude": 4.90 }"""

        webTestClient
            .withDevicePrincipal()
            .post()
            .uri("/api/trackables/$trackableId/$deviceId/position")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isNotFound

        verify(positionService, times(1)).report(eq(trackableId), eq(deviceId), any())
        verifyNoMoreInteractions(positionService)
    }

    @Test
    fun `should fail multiple positions when device not found`() {
        val trackableId = "trackableId"
        val deviceId = "deviceId"

        whenever(positionService.report(eq(trackableId), eq(deviceId), any()))
            .thenReturn(Flux.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found for trackable")))

        val body = """[{ "latitude": 52.37, "longitude": 4.90, "createdAt": "2010-01-01T00:00:00Z" }]"""

        webTestClient
            .withDevicePrincipal()
            .post()
            .uri("/api/trackables/$trackableId/$deviceId/positions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isNotFound

        verify(positionService, times(1)).report(eq(trackableId), eq(deviceId), any())
        verifyNoMoreInteractions(positionService)
    }

    @Test
    fun `should fail when position created at is in the future`() {
        val trackableId = "t-123"
        val deviceId = "d-456"

        val body = """
          { "latitude": 52.37, "longitude": 4.90, "createdAt": "2999-01-01T00:00:00Z" }
        """.trimIndent()

        webTestClient
            .withDevicePrincipal()
            .post()
            .uri("/api/trackables/$trackableId/$deviceId/position")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.error").isEqualTo("validation_error")
            .jsonPath("$.fields.createdAt").exists()

        verifyNoMoreInteractions(positionService)
    }

    @Test
    fun `should fail multiple positions when position created at is in the future`() {
        val trackableId = "t-123"
        val deviceId = "d-456"

        val body = """
          [{ "latitude": 52.37, "longitude": 4.90, "createdAt": "2999-01-01T00:00:00Z" }]
        """.trimIndent()

        webTestClient
            .withDevicePrincipal()
            .post()
            .uri("/api/trackables/$trackableId/$deviceId/positions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isBadRequest

        verifyNoMoreInteractions(positionService)
    }



    @Test
    fun `should throw error when device not found`() {
        whenever(positionService.report(any(), any(), any()))
            .thenReturn(Flux.error(DeviceNotFoundException("Device not found for trackable")))

        webTestClient
            .withDevicePrincipal()
            .post()
            .uri("/api/trackables/track-1/dev-1/position")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"latitude": 1.0, "longitude": 2.0}""")
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.error").isEqualTo("device_not_found")
            .jsonPath("$.message").isEqualTo("Device not found for trackable")
            .jsonPath("$.fields").doesNotExist()
    }

    @Test
    fun `should throw error when created at is in the future`() {
        whenever(positionService.report(any(), any(), any()))
            .thenReturn(Flux.error(PositionInFutureException("createdAt must not be in the future")))

        webTestClient
            .withDevicePrincipal()
            .post()
            .uri("/api/trackables/track-1/dev-1/position")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"latitude": 1.0, "longitude": 2.0, "createdAt": "2010-01-01T00:00:00Z"}""")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.error").isEqualTo("position_in_future")
            .jsonPath("$.message").isEqualTo("createdAt must not be in the future")
            .jsonPath("$.fields").doesNotExist()
    }

    @Test
    fun `should throw error when lat or long is out of bounds`() {
        whenever(positionService.report(any(), any(), any()))
            .thenReturn(Flux.error(InvalidCoordinateException("latitude must be in [-90, 90] and longitude in [-180, 180]")))

        webTestClient
            .withDevicePrincipal()
            .post()
            .uri("/api/trackables/track-1/dev-1/position")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"latitude": 1.0, "longitude": 2.0}""")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.error").isEqualTo("invalid_coordinate")
            .jsonPath("$.fields").doesNotExist()
    }

}
