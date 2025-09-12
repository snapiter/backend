package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.devices.Device
import com.snapiter.backend.model.trackable.devices.DeviceRepository
import com.snapiter.backend.model.trackable.positionreport.PositionReport
import com.snapiter.backend.model.trackable.positionreport.PositionReportRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito.given
import org.mockito.Mockito.*
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.ZoneOffset

@WebFluxTest(controllers = [PositionReportController::class])
class PositionReportControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var deviceRepository: DeviceRepository

    @MockitoBean
    lateinit var positionReportRepository: PositionReportRepository

    @Test
    fun `POST single position returns 204 when device exists`() {
        val trackableId = "t-123"
        val deviceId = "d-456"

        val device = mock(Device::class.java)
        given(deviceRepository.findByDeviceIdAndTrackableId(deviceId, trackableId))
            .willReturn(Mono.just(device))

        given(positionReportRepository.save(any(PositionReport::class.java)))
            .willReturn(Mono.just(mock(PositionReport::class.java)))

        val body = """
            {
              "latitude": 52.37,
              "longitude": 4.90,
              "createdAt": "2025-09-12T09:00:00Z"
            }
        """.trimIndent()

        webTestClient.post()
            .uri("/api/trackable/$trackableId/$deviceId/position")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isNoContent

        // Verify interactions
        verify(deviceRepository, times(1)).findByDeviceIdAndTrackableId(deviceId, trackableId)
        verify(positionReportRepository, times(1)).save(any(PositionReport::class.java))
        verifyNoMoreInteractions(deviceRepository, positionReportRepository)
    }

    @Test
    fun `POST single position returns 404 when device not found`() {
        val trackableId = "t-unknown"
        val deviceId = "d-unknown"

        // Device does not exist
        given(deviceRepository.findByDeviceIdAndTrackableId(deviceId, trackableId))
            .willReturn(Mono.empty())

        val body = """
            {
              "latitude": 52.37,
              "longitude": 4.90
            }
        """.trimIndent()

        webTestClient.post()
            .uri("/api/trackable/$trackableId/$deviceId/position")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isNotFound

        verify(deviceRepository, times(1)).findByDeviceIdAndTrackableId(deviceId, trackableId)
        verify(positionReportRepository, never()).save(any(PositionReport::class.java))
        verifyNoMoreInteractions(deviceRepository, positionReportRepository)
    }
    @Test
    fun `POST uses client createdAt when provided`() {
        val trackableId = "t-123"
        val deviceId = "d-456"
        val clientTs = OffsetDateTime.parse("2025-09-12T09:00:00Z")

        val device = mock(Device::class.java)
        given(deviceRepository.findByDeviceIdAndTrackableId(deviceId, trackableId))
            .willReturn(Mono.just(device))

        val captor = ArgumentCaptor.forClass(PositionReport::class.java)
        given(positionReportRepository.save(captor.capture()))
            .willReturn(Mono.just(mock(PositionReport::class.java)))

        val body = """
        {
          "latitude": 52.3702,
          "longitude": 4.8952,
          "createdAt": "$clientTs"
        }
    """.trimIndent()

        webTestClient.post()
            .uri("/api/trackable/$trackableId/$deviceId/position")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isNoContent

        val saved = captor.value
        assertEquals(clientTs.toInstant(), saved.createdAt!!.toInstant(ZoneOffset.UTC))
    }
}
