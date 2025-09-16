package com.snapiter.backend.security

import com.snapiter.backend.model.users.UserEntity
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import java.time.Duration

@Service
class JwtService(@Value("\${app.jwt.secret}") private val secret: String,
                 @Value("\${app.jwt.issuer}") private val issuer: String,
                 @Value("\${app.jwt.access-ttl-minutes}") private val accessTtl: Long) {

    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    data class Principal(val userId: UUID, val email: String) {
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
    }

    fun issueAccessToken(user: UserEntity): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(user.userId.toString())
            .issuer(issuer)
            .claim("email", user.email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(Duration.ofMinutes(accessTtl))))
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }

    fun parse(token: String): Mono<Principal> = Mono.fromCallable {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        Principal(UUID.fromString(claims.subject), claims["email", String::class.java])
    }
}
