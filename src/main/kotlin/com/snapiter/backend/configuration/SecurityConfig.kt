package com.snapiter.backend.configuration

import com.snapiter.backend.model.trackable.devices.DeviceRepository
import com.snapiter.backend.model.trackable.devices.tokens.DeviceTokenService
import com.snapiter.backend.security.DeviceAuthWebFilter
import com.snapiter.backend.security.JwtAuthWebFilter
import com.snapiter.backend.security.JwtService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.http.HttpMethod
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    private val jwtService: JwtService,
    private val deviceTokenService: DeviceTokenService,
    private val deviceRepository: DeviceRepository
) {

    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val corsConfig = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("*")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", corsConfig)
        }

        return CorsWebFilter(source)
    }

    @Bean
    fun apiSecurityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            // Apply this chain ONLY to /api/**
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/api/**"))
            .cors { it.disable() }
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeExchange {
                it.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.pathMatchers(HttpMethod.GET,
                    "/api/trackables/*/trips",
                    "/api/trackables/*/trips/*",
                    "/api/trackables/*/trips/*/positions",
                    "/api/trackables/*/positions",
                    "/api/trackables/host/**"
                ).permitAll()
                it.pathMatchers(
                    "/api/auth/login/email/request",
                    "/api/auth/login/email/consume",
                    "/api/auth/refresh",
                    "/api/auth/logout",
                ).permitAll()

                it.pathMatchers("/api/**").authenticated()
            }
            .addFilterAt(DeviceAuthWebFilter(deviceTokenService, deviceRepository), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAt(JwtAuthWebFilter(jwtService), SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }


}
