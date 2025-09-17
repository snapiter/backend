package com.snapiter.backend.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class JwtAuthWebFilter(private val jwt: JwtService) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        if (authHeader?.startsWith("Bearer ") == true) {
            val token = authHeader.removePrefix("Bearer ").trim()
            return jwt.parse(token)
                .flatMap { principal ->
                    val authentication = UsernamePasswordAuthenticationToken(
                        UserPrincipal(principal.userId, principal.email),
                        token,
                        listOf(SimpleGrantedAuthority("ROLE_USER"))
                    )
                    chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                }
                .onErrorResume { error ->
                    when (error) {
                        is ExpiredJwtException, is JwtException -> {
                            val response = exchange.response
                            response.statusCode = HttpStatus.UNAUTHORIZED
                            response.headers.add("Content-Type", "application/json")

                            val errorBody = """{"error":"expired_token","message":"Token expired"}"""
                            val buffer = response.bufferFactory().wrap(errorBody.toByteArray())
                            response.writeWith(Mono.just(buffer))
                        }
                        else -> {
                            // Let other errors bubble up
                            Mono.error(error)
                        }
                    }
                }
        }
        return chain.filter(exchange)
    }
}

