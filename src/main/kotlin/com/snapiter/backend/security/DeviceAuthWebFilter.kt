package com.snapiter.backend.security

import com.snapiter.backend.model.trackable.devices.tokens.DeviceTokenService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class DeviceAuthWebFilter(private val tokenSvc: DeviceTokenService) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val raw = exchange.request.headers.getFirst("X-Device-Token")?.trim()
            ?: return chain.filter(exchange) // no header → let request continue unauthenticated

        return tokenSvc.validate(raw).flatMap { deviceId ->
            val authn = UsernamePasswordAuthenticationToken(
                DevicePrincipal(deviceId),
                raw,
                listOf(SimpleGrantedAuthority("ROLE_DEVICE"))
            )
            chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authn))
        }
    }
}
