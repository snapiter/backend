package com.snapiter.backend.api

import com.snapiter.backend.security.JwtService
import com.snapiter.backend.security.MagicLinkService
import com.snapiter.backend.security.RefreshTokenService
import com.snapiter.backend.security.Tokens
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/auth/magic")
class MagicAuthController(
    private val magic: MagicLinkService,
    private val jwt: JwtService,
    private val refreshTokenService: RefreshTokenService
) {
    data class MagicRequest(val email: String)
    data class ConsumeRequest(val token: String)

    @PostMapping("/request")
    fun request(@RequestBody body: MagicRequest): Mono<ResponseEntity<Void>> =
        magic.requestLink(body.email)
            .thenReturn(ResponseEntity.ok().build())


    @PostMapping("/consume")
    fun consume(@RequestBody body: ConsumeRequest, exchange: ServerWebExchange): Mono<Tokens> =
        magic.consume(body.token).flatMap { user ->
            refreshTokenService.startSession(user, exchange)
        }

}
