package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.trackable.TrackableService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest(controllers = [TrackableController::class])
class TrackableControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var service: TrackableService

    @Test
    fun `POST create returns 201 and saves entity with createdAt`() {
        val captor = argumentCaptor<CreateTrackableRequest>()
        whenever(service.createTracker(any())).thenReturn(Mono.just("trackableId"))

        val body = """
            {
              "name": "SnapIter name",
              "websiteTitle": "SnapIter",
              "website": "https://snapiter.com",
              "hostName": "snapiter.eu",
              "icon": "📍"
            }
        """.trimIndent()

        // Act + Assert (HTTP)
        webTestClient.post()
            .uri("/api/trackables")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isCreated
            .expectHeader().valueMatches("Location", "/api/trackables/.*")

        // Assert (repo interaction & saved entity)
        verify(service, times(1)).createTracker(captor.capture())
        val saved = captor.firstValue

        assertEquals("SnapIter name", saved.name)
        assertEquals("SnapIter", saved.websiteTitle)
        assertEquals("https://snapiter.com", saved.website)
        assertEquals("snapiter.eu", saved.hostName)
        assertEquals("📍", saved.icon)

    }
}
