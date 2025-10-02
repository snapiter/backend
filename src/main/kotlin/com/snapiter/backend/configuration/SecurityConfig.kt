package com.snapiter.backend.configuration

import com.snapiter.backend.model.trackable.devices.DeviceRepository
import com.snapiter.backend.model.trackable.devices.tokens.DeviceTokenService
import com.snapiter.backend.security.DeviceAuthWebFilter
import com.snapiter.backend.security.JwtAuthWebFilter
import com.snapiter.backend.security.JwtService
import com.snapiter.backend.security.TrackableSecurityService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

import org.springframework.security.authorization.AuthorizationDecision
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableScheduling
class SecurityConfig(
    private val jwtService: JwtService,
    private val deviceTokenService: DeviceTokenService,
    private val deviceRepository: DeviceRepository,
    private val trackableAccessChecker: TrackableSecurityService
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
    // Specific BEAN for registering a device, at this point it does NOT have any token yet.
    fun deviceRegisterSecurityChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .securityMatcher(
                ServerWebExchangeMatchers.pathMatchers(
                    HttpMethod.POST,
                    "/api/trackables/*/devices/register"
                )
            )
            .csrf { it.disable() }
            .cors { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges.anyExchange().permitAll()
            }
            .build()
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
                it.pathMatchers(
                    HttpMethod.GET,
                    "/api/trackables/*/trips",
                    "/api/trackables/*/trips/*",
                    "/api/trackables/*/trips/*/positions",
                    "/api/trackables/*/trips/*/markers",
                    "/api/trackables/*/markers/**",
                    "/api/trackables/*/positions",
                    "/api/trackables/host/**",
                    "/api/trackables/*/icon"
                ).permitAll()
                it.pathMatchers(
                    HttpMethod.POST,
                    "/api/auth/login/email/request",
                    "/api/auth/login/email/consume",
                    "/api/auth/refresh",
                    "/api/auth/logout",
                ).permitAll()

                it.pathMatchers("/api/trackables/{trackableId}/**")
                    .access { authenticationMono, context ->
                        val trackableId = context.variables["trackableId"]

                        authenticationMono.flatMap { authentication ->
                            if (trackableId == null) {
                                // deny if we didn't get the variable
                                Mono.just(AuthorizationDecision(false))
                            } else {
                                trackableAccessChecker.canAccess(trackableId as String, authentication)
                                    .map { granted -> AuthorizationDecision(granted) }
                            }
                        }
                    }


                it.pathMatchers("/api/**").authenticated()
            }
            .addFilterAt(
                DeviceAuthWebFilter(deviceTokenService, deviceRepository),
                SecurityWebFiltersOrder.AUTHENTICATION
            )
            .addFilterAt(JwtAuthWebFilter(jwtService), SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }


}
