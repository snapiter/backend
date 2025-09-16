package com.snapiter.backend

import com.snapiter.backend.security.DevicePrincipal
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.core.publisher.Mono

@TestConfiguration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class TestSecurityConfig {

    @Bean
    fun testAuthenticationManager(): ReactiveAuthenticationManager {
        return ReactiveAuthenticationManager { authentication ->
            val principal = when (authentication.name) {
                "device" -> DevicePrincipal("test-device-id")
                else -> DevicePrincipal("default-device-id")
            }

            val authenticatedToken = UsernamePasswordAuthenticationToken(
                principal,
                authentication.credentials,
                listOf(SimpleGrantedAuthority("ROLE_DEVICE"))
            )
            Mono.just(authenticatedToken)
        }
    }

    @Bean
    fun testSecurityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .authenticationManager(testAuthenticationManager())
            .authorizeExchange { it.anyExchange().authenticated() }
            .build()
}
