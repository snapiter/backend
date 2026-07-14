package com.snapiter.backend.api.trackable

import com.snapiter.backend.api.GlobalExceptionHandler
import com.snapiter.backend.model.trackable.devices.DeviceNotFoundException
import com.snapiter.backend.model.trackable.positionreport.PositionService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux

class PositionErrorResponseTest {
    private val positionService: PositionService = mock()
    private val client: WebTestClient =
        WebTestClient.bindToController(PositionController(positionService))
            .controllerAdvice(GlobalExceptionHandler())
            .build()

    @Test
    fun `should map device not found to the standard error response`() {
        whenever(positionService.report(any(), any(), any()))
            .thenReturn(Flux.error(DeviceNotFoundException("Device not found for trackable")))

        client.post()
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
}
