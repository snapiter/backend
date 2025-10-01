package com.snapiter.backend.api

import com.snapiter.backend.security.MagicLinkService
import com.snapiter.backend.security.RefreshTokenService
import com.snapiter.backend.security.Tokens
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/auth/login/email")
@Tag(
    name = "Authentication - Email Links",
    description = "Endpoints to request and consume email links that generate access tokens"
)
class AuthController(
    private val magic: MagicLinkService,
    private val refreshTokenService: RefreshTokenService
) {
    data class MagicRequest(
        @Schema(description = "The email address to send the link to", example = "user@example.com")
        val email: String
    )

    data class ConsumeRequest(
        @Schema(description = "The link token received via email", example = "eyJhbGciOiJIUzI1NiIs...")
        val token: String
    )

    @Operation(
        summary = "Request a link",
        description = "Send a login link to the provided email address",
        requestBody = SwaggerRequestBody(
            required = true,
            content = [Content(schema = Schema(implementation = MagicRequest::class))]
        ),
        responses = [
            ApiResponse(responseCode = "200", description = "Link has been sent"),
            ApiResponse(responseCode = "400", description = "Invalid email address", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Internal server error", content = [Content()])
        ]
    )
    @PostMapping("/request")
    fun request(@Valid @RequestBody body: MagicRequest): Mono<ResponseEntity<Void>> =
        magic.requestLink(body.email)
            .thenReturn(ResponseEntity.ok().build())


    @Operation(
        summary = "Consume a token",
        description = "Exchange a link token for access and refresh tokens",
        requestBody = SwaggerRequestBody(
            required = true,
            content = [Content(schema = Schema(implementation = ConsumeRequest::class))]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully authenticated, returns JWT tokens",
                content = [Content(schema = Schema(implementation = Tokens::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid or expired token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Internal server error", content = [Content()])
        ]
    )
    @PostMapping("/consume")
    fun consume(@Valid @RequestBody body: ConsumeRequest, exchange: ServerWebExchange): Mono<Tokens> =
        magic.consume(body.token).flatMap { user ->
            refreshTokenService.startSession(user, exchange)
        }
}
