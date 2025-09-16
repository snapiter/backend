package com.snapiter.backend.configuration

import com.snapiter.backend.model.trackable.devices.tokens.DeviceTokenService
import com.snapiter.backend.security.DeviceAuthWebFilter
import com.snapiter.backend.security.JwtAuthWebFilter
import com.snapiter.backend.security.JwtService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val jwtService: JwtService,
    private val deviceTokenService: DeviceTokenService
) {

    @Bean
    fun apiSecurityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            // Apply this chain ONLY to /api/**
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/api/**"))
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeExchange {
                it.pathMatchers(
                    "/api/auth/login/email/request",
                    "/api/auth/login/email/consume",
                    "/api/auth/refresh",
                    "/api/auth/logout"
                ).permitAll()
                it.pathMatchers("/api/**").authenticated()
            }
            .addFilterAt(DeviceAuthWebFilter(deviceTokenService), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAt(JwtAuthWebFilter(jwtService), SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }
}
