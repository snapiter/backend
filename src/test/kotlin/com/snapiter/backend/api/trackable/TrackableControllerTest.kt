package com.snapiter.backend.api.trackable

import com.snapiter.backend.model.trackable.trackable.Trackable
import com.snapiter.backend.model.trackable.trackable.TrackableRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito.given
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDateTime

@WebFluxTest(controllers = [TrackableController::class])
class TrackableControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var repository: TrackableRepository

    @Test
    fun `POST create returns 201 and saves entity with createdAt`() {
        // Arrange
        val captor = ArgumentCaptor.forClass(Trackable::class.java)

        // Echo back the saved entity
        given(repository.save(any()))
            .willAnswer { invocation -> Mono.just(invocation.arguments[0] as Trackable) }

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
        verify(repository, times(1)).save(captor.capture())
        val saved = captor.value

        assertEquals("SnapIter name", saved.name)
        assertEquals("SnapIter", saved.websiteTitle)
        assertEquals("https://snapiter.com", saved.website)
        assertEquals("snapiter.eu", saved.hostName)
        assertEquals("📍", saved.icon)
        assertNotNull(saved.trackableId)

        // createdAt is set server-side and close to now
        val createdAt = saved.createdAt
        assertTrue(createdAt != null, "createdAt should be set by the server")
        val seconds = Duration.between(createdAt, LocalDateTime.now()).abs().seconds
        assertTrue(seconds < 5, "createdAt should be close to now (was ${'$'}seconds s diff)")
    }
}
