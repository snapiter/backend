package com.snapiter.backend.model.trackable.devices.tokens

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class DeviceTokenServiceTest {

    private lateinit var repo: DeviceTokenRepository
    private lateinit var service: DeviceTokenService

    @BeforeEach
    fun setup() {
        repo = mock(DeviceTokenRepository::class.java)
        service = DeviceTokenService(repo)
    }

    @Test
    fun `issue should revoke existing tokens and save new token`() {
        val trackableId = "track123"
        val existing = DeviceToken(
            id = 1L,
            trackableId = trackableId,
            deviceId = null,
            tokenHash = "oldhash",
            createdAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1),
            revokedAt = null
        )

        // repo returns existing token
        `when`(repo.findByTrackableIdAndDeviceIdNull(trackableId))
            .thenReturn(Flux.just(existing))
        `when`(repo.save(any<DeviceToken>()))
            .thenAnswer { Mono.just(it.arguments[0] as DeviceToken) }

        val result = service.issue(trackableId)

        StepVerifier.create(result)
            .assertNext { raw ->
                assertNotNull(raw) // raw token returned
                assertTrue(raw.length > 10)
            }
            .verifyComplete()

        val captor = argumentCaptor<DeviceToken>()
        verify(repo, atLeastOnce()).save(captor.capture())
        val saved = captor.allValues

        // First save should be revocation of existing token
        assertTrue(saved.any { it.id == existing.id && it.revokedAt != null })
        // Another save should be the new token
        assertTrue(saved.any { it.trackableId == trackableId && it.revokedAt == null })
    }

    @Test
    fun `should assign device to token`() {
        val token = DeviceToken(
            id = 123L,
            trackableId = "track",
            deviceId = null,
            tokenHash = "hash",
            createdAt = OffsetDateTime.now(ZoneOffset.UTC),
            revokedAt = null
        )

        val updated = token.copy(deviceId = "dev123")

        `when`(repo.save(any<DeviceToken>())).thenReturn(Mono.just(updated))

        val result = service.assignDeviceToToken(token, "dev123")

        StepVerifier.create(result)
            .expectNext(updated)
            .verifyComplete()

        verify(repo).save(eq(updated))
    }

    @Test
    fun `should return valid token`() {
        val raw = "someraw"
        val hashed = callSha256(service, raw)

        val token = DeviceToken(
            id = 1L,
            trackableId = "track",
            deviceId = "dev",
            tokenHash = hashed,
            createdAt = OffsetDateTime.now(ZoneOffset.UTC),
            revokedAt = null
        )

        `when`(repo.findByTokenHash(hashed)).thenReturn(Mono.just(token))

        StepVerifier.create(service.validate(raw))
            .expectNext(token)
            .verifyComplete()
    }

    @Test
    fun `validate should error if revoked`() {
        val raw = "rawtoken"
        val hashed = callSha256(service, raw)

        val token = DeviceToken(
            id = 2L,
            trackableId = "track",
            deviceId = "dev",
            tokenHash = hashed,
            createdAt = OffsetDateTime.now(ZoneOffset.UTC),
            revokedAt = OffsetDateTime.now(ZoneOffset.UTC)
        )

        `when`(repo.findByTokenHash(hashed)).thenReturn(Mono.just(token))

        StepVerifier.create(service.validate(raw))
            .expectError(UnauthorizedTokenException::class.java)
            .verify()
    }

    @Test
    fun `should throw error on invalid token`() {
        val raw = "nonexistent"
        val hashed = callSha256(service, raw)

        `when`(repo.findByTokenHash(hashed)).thenReturn(Mono.empty())

        StepVerifier.create(service.validate(raw))
            .expectError(UnauthorizedTokenException::class.java)
            .verify()
    }

    // Helper to access private sha256 for consistent test values
    private fun callSha256(service: DeviceTokenService, raw: String): String {
        val m = service::class.java.getDeclaredMethod("sha256", String::class.java)
        m.isAccessible = true
        return m.invoke(service, raw) as String
    }
}
