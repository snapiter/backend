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

    fun issue(trackableId: String): Mono<String> {
        val raw = newRaw()
        val hash = sha256(raw)
        val row = DeviceToken(null, trackableId, null,hash, now(), null)

        return repo.findByTrackableId(trackableId)
            // Revoke all older
            .flatMap { existing ->
                repo.save(existing.copy(revokedAt = now()))
            }
            .then(repo.save(row))
            .thenReturn(raw)

    }

    fun assignDeviceToToken(deviceToken: DeviceToken, deviceId: String): Mono<DeviceToken> {
        return repo.save(deviceToken.copy(deviceId = deviceId))
    }


    fun validate(raw: String): Mono<DeviceToken> =
        repo.findByTokenHash(sha256(raw))
            .filter { it.revokedAt == null }
            .switchIfEmpty(Mono.error(UnauthorizedTokenException("invalid_device_token")))
}

class UnauthorizedTokenException(msg: String) : RuntimeException(msg)
class UnclaimedTokenNotFound(msg: String) : RuntimeException(msg)
