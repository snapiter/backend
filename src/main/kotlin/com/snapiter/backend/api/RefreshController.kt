package com.snapiter.backend.api

import com.snapiter.backend.security.RefreshTokenService
import com.snapiter.backend.security.Tokens
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/auth")
@Tag(
    name = "Authentication - Refresh & Logout",
    description = "Endpoints to refresh JWT tokens and to log out a session"
)
class RefreshController(
    private val refreshSvc: RefreshTokenService
) {
    @Operation(
        summary = "Refresh tokens",
        description = "Exchanges a valid refresh token (sent via cookie) for a new access token and refresh token.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully refreshed, returns new tokens",
                content = [Content(schema = Schema(implementation = Tokens::class))]
            ),
            ApiResponse(responseCode = "401", description = "Invalid or expired refresh token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Internal server error", content = [Content()])
        ]
    )
    @PostMapping("/refresh")
    fun refresh(exchange: ServerWebExchange): Mono<Tokens> =
        refreshSvc.refresh(exchange)

    @Operation(
        summary = "Logout",
        description = "Logs out the user by invalidating their refresh token session. Typically clears cookies.",
        responses = [
            ApiResponse(responseCode = "204", description = "Successfully logged out, no content"),
            ApiResponse(responseCode = "401", description = "No valid session found", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Internal server error", content = [Content()])
        ]
    )
    @PostMapping("/logout")
    fun logout(exchange: ServerWebExchange): Mono<ResponseEntity<Void>> =
        refreshSvc.logout(exchange).thenReturn(ResponseEntity.noContent().build())
}
