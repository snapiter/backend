package com.snapiter.backend.api

import com.snapiter.backend.security.RefreshTokenService
import com.snapiter.backend.security.Tokens
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/auth")
class RefreshController(
    private val refreshSvc: RefreshTokenService
) {
    @PostMapping("/refresh")
    fun refresh(exchange: ServerWebExchange): Mono<Tokens> =
        refreshSvc.refresh(exchange)

    @PostMapping("/logout")
    fun logout(exchange: ServerWebExchange): Mono<ResponseEntity<Void>> =
        refreshSvc.logout(exchange).thenReturn(ResponseEntity.noContent().build())
}
