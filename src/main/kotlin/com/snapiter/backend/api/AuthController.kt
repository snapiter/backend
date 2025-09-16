package com.snapiter.backend.api

import com.snapiter.backend.security.JwtService
import com.snapiter.backend.security.MagicLinkService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/auth/magic")
class MagicAuthController(
    private val magic: MagicLinkService,
    private val jwt: JwtService
) {
    data class MagicRequest(val email: String)
    data class ConsumeRequest(val token: String)
    data class Tokens(val accessToken: String)

    @PostMapping("/request")
    fun request(@RequestBody body: MagicRequest): Mono<ResponseEntity<Void>> =
        magic.requestLink(body.email)
            .thenReturn(ResponseEntity.ok().build())

    @PostMapping("/consume")
    fun consume(@RequestBody body: ConsumeRequest): Mono<Tokens> =
        magic.consume(body.token).map { user ->
            val access = jwt.issueAccessToken(user)
            // Option A: also set a refresh token cookie (not shown; add your own RefreshTokenService)
            Tokens(access)
        }
}
