package com.snapiter.backend.api.trackable

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
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

@WebFluxTest(controllers = [PositionController::class])
class PositionControllerTest {

    @Autowired lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var positionService: PositionService

    @Test
    fun `POST single position returns 204 when device exists`() {
        val trackableId = "t-123"
        val deviceId = "d-456"

        whenever(positionService.report(eq(trackableId), eq(deviceId), any()))
            .thenReturn(Mono.empty())

        val body = """
          { "latitude": 52.37, "longitude": 4.90, "createdAt": "2025-09-12T09:00:00Z" }
        """.trimIndent()

        webTestClient.post()
            .uri("/api/trackable/$trackableId/$deviceId/position")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isNoContent

        verify(positionService, times(1)).report(eq(trackableId), eq(deviceId), any())
        verifyNoMoreInteractions(positionService)
    }

    @Test
    fun `POST single position returns 404 when device not found`() {
        val trackableId = "t-unknown"
        val deviceId = "d-unknown"

        whenever(positionService.report(eq(trackableId), eq(deviceId), any()))
            .thenReturn(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found for trackable")))

        val body = """{ "latitude": 52.37, "longitude": 4.90 }"""

        webTestClient.post()
            .uri("/api/trackable/$trackableId/$deviceId/position")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isNotFound

        verify(positionService, times(1)).report(eq(trackableId), eq(deviceId), any())
        verifyNoMoreInteractions(positionService)
    }

    @Test
    fun `POST uses client createdAt when provided`() {
        val trackableId = "t-123"
        val deviceId = "d-456"
        val clientTs = OffsetDateTime.parse("2025-09-12T09:00:00Z")

        val reqCaptor = argumentCaptor<PositionRequest>()

        whenever(positionService.report(eq(trackableId), eq(deviceId), reqCaptor.capture()))
            .thenReturn(Mono.empty())

        val body = """
          { "latitude": 52.3702, "longitude": 4.8952, "createdAt": "$clientTs" }
        """.trimIndent()

        webTestClient.post()
            .uri("/api/trackable/$trackableId/$deviceId/position")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isNoContent

        assertEquals(clientTs, reqCaptor.firstValue.createdAt)
    }
}
