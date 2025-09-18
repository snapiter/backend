package com.snapiter.backend.security

import com.snapiter.backend.model.trackable.devices.DeviceRepository
import com.snapiter.backend.model.trackable.devices.tokens.DeviceTokenService
import com.snapiter.backend.model.trackable.devices.tokens.UnauthorizedTokenException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class DeviceAuthWebFilter(
    private val tokenSvc: DeviceTokenService,
    private val deviceRepo: DeviceRepository
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val raw = exchange.request.headers.getFirst("X-Device-Token")?.trim()
            ?: return chain.filter(exchange) // no header → unauthenticated


        return tokenSvc.validate(raw) // validate token
            .filter { it.deviceId == null } // only pass through active deviceIds
            .switchIfEmpty(Mono.error(UnauthorizedRefreshTokenException("device_token_invalid")))
            .flatMap { deviceToken ->
                val deviceId = deviceToken.deviceId
                    ?: return@flatMap Mono.error(UnauthorizedTokenException("missing_device_id"))

                deviceRepo.findUserIdByDeviceId(deviceId)
                    .map { userId ->
                        UsernamePasswordAuthenticationToken(
                            DevicePrincipal(userId, deviceId),
                            raw,
                            listOf(SimpleGrantedAuthority("ROLE_DEVICE"))
                        )
                    }
            }
            .flatMap { authn ->
                chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authn))
            }
    }
}
