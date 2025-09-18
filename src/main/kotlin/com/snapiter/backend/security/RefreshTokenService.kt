package com.snapiter.backend.security

import com.snapiter.backend.model.users.RefreshToken
import com.snapiter.backend.model.users.RefreshTokenRepository
import com.snapiter.backend.model.users.User
import com.snapiter.backend.model.users.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class RefreshTokenService(
    private val repo: RefreshTokenRepository,
    private val userRepo: UserRepository,
    private val jwt: JwtService,
    @Value("\${app.jwt.refresh-ttl-days}") private val refreshTtlDays: Long
) {
    private val rnd = SecureRandom()


    private fun nowUtc() = OffsetDateTime.now(ZoneOffset.UTC)

    private fun newRawToken(): String {
        val buf = ByteArray(32); rnd.nextBytes(buf)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf) // opaque string
    }

    private fun sha256(s: String): String {
        val d = MessageDigest.getInstance("SHA-256")
        return Base64.getUrlEncoder().withoutPadding().encodeToString(d.digest(s.toByteArray()))
    }

    fun buildCookie(rawRefresh: String, maxAgeDays: Long, secure: Boolean = true): ResponseCookie =
        ResponseCookie.from("refresh_token", rawRefresh)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/")
            .maxAge(maxAgeDays * 24 * 60 * 60)
            .build()

    fun clearCookie(): ResponseCookie =
        ResponseCookie.from("refresh_token", "")
            .httpOnly(true).secure(true).sameSite("Lax").path("/").maxAge(0).build()

    fun startSession(user: User, exchange: ServerWebExchange): Mono<Tokens> {
        val raw = newRawToken()
        val hash = sha256(raw)
        val now = nowUtc()
        val entity = RefreshToken(
            userId = user.userId,
            tokenHash = hash,
            issuedAt = now,
            expiresAt = now.plusDays(refreshTtlDays),
            userAgent = exchange.request.headers.getFirst("User-Agent"),
            ip = exchange.request.remoteAddress?.address?.hostAddress
        )
        val cookie = buildCookie(raw, refreshTtlDays)
        exchange.response.addCookie(cookie)

        val access = jwt.issueAccessToken(user)
        return repo.save(entity).thenReturn(Tokens(access))
    }

    fun refresh(exchange: ServerWebExchange): Mono<Tokens> {
        val raw = exchange.request.cookies.getFirst("refresh_token")?.value
            ?: return Mono.error(UnauthorizedRefreshTokenException("missing_refresh_token"))
        val hash = sha256(raw)
        val now = nowUtc()

        return repo.findByTokenHash(hash)
            .switchIfEmpty(Mono.error(UnauthorizedRefreshTokenException("invalid_refresh_token")))
            .flatMap { rt ->
                if (rt.revokedAt != null) return@flatMap Mono.error(UnauthorizedRefreshTokenException("revoked_refresh_token"))
                if (now.isAfter(rt.expiresAt)) return@flatMap Mono.error(UnauthorizedRefreshTokenException("expired_refresh_token"))
                if (rt.replacedBy != null) return@flatMap Mono.error(UnauthorizedRefreshTokenException("reused_refresh_token"))

                // Load user
                userRepo.findByUserId(rt.userId).switchIfEmpty(Mono.error(UnauthorizedRefreshTokenException("user_not_found")))
                    .flatMap { user ->
                        val childRaw = newRawToken()
                        val childHash = sha256(childRaw)
                        val child = RefreshToken(
                            userId = user.userId,
                            tokenHash = childHash,
                            issuedAt = now,
                            expiresAt = now.plusDays(refreshTtlDays),
                            userAgent = exchange.request.headers.getFirst("User-Agent"),
                            ip = exchange.request.remoteAddress?.address?.hostAddress
                        )
                        val updatedParent = rt.copy(
                            revokedAt = now,
                            replacedBy = childHash,
                            lastUsedAt = now
                        )
                        val cookie = buildCookie(childRaw, refreshTtlDays)
                        exchange.response.addCookie(cookie)

                        val access = jwt.issueAccessToken(user)
                        repo.save(updatedParent)
                            .then(repo.save(child))
                            .thenReturn(Tokens(access))
                    }
            }
    }

    fun logout(exchange: ServerWebExchange): Mono<Void> {
        val cookie = exchange.request.cookies.getFirst("refresh_token")
        if (cookie == null) {
            exchange.response.addCookie(clearCookie())
            return Mono.empty()
        }

        val raw = cookie.value
        val hash = sha256(raw)
        val now = nowUtc()
        exchange.response.addCookie(clearCookie())

        return repo.findByTokenHash(hash)
            .flatMap { rt -> repo.save(rt.copy(revokedAt = now)) }
            .then()
    }

}

data class Tokens(val accessToken: String)
class UnauthorizedRefreshTokenException(msg: String) : RuntimeException(msg)
