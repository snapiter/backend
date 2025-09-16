package com.snapiter.backend.model.trackable.devices.tokens

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Base64

@Service
class DeviceTokenService(private val repo: DeviceTokenRepository) {
    private val rnd = SecureRandom()
    private fun now() = OffsetDateTime.now(ZoneOffset.UTC)

    private fun newRaw(): String {
        val b = ByteArray(32); rnd.nextBytes(b)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b)
    }
    private fun sha256(s: String): String {
        val d = MessageDigest.getInstance("SHA-256")
        return Base64.getUrlEncoder().withoutPadding().encodeToString(d.digest(s.toByteArray()))
    }

    /** Issue (or rotate) a token for this device; returns RAW token (show once). */
    fun issue(deviceId: String): Mono<String> {
        val raw = newRaw()
        val hash = sha256(raw)
        val row = DeviceToken(null, deviceId, hash, now(), null)

        return repo.findByDeviceId(deviceId)
            .flatMap { existing ->
                val revoked = existing.copy(revokedAt = now())
                repo.save(revoked).then(repo.save(row))
            }
            .switchIfEmpty(repo.save(row))
            .thenReturn(raw)
    }

    fun revoke(deviceId: String): Mono<Void> =
        repo.findByDeviceId(deviceId)
            .flatMap { repo.save(it.copy(revokedAt = now())) }
            .then()

    /** Validate raw token → returns deviceId if valid & not revoked. */
    fun validate(raw: String): Mono<String> =
        repo.findByTokenHash(sha256(raw))
            .filter { it.revokedAt == null }
            .map { it.deviceId }
            .switchIfEmpty(Mono.error(UnauthorizedException("invalid_device_token")))
}

class UnauthorizedException(msg: String) : RuntimeException(msg)
