package com.snapiter.backend

import com.snapiter.backend.security.DevicePrincipal
import com.snapiter.backend.security.UserPrincipal
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*

object TestAuthUtils {
    fun WebTestClient.withDevicePrincipal(deviceId: String = "test-device-id"): WebTestClient {
        val devicePrincipal = DevicePrincipal(deviceId)
        val authentication = UsernamePasswordAuthenticationToken(
            devicePrincipal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_DEVICE"))
        )
        return this.mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
    }

    fun WebTestClient.withUserPrincipal(
        userId: UUID = UUID.randomUUID(),
        email: String = "test@example.com"
    ): WebTestClient {
        val userPrincipal = UserPrincipal(userId, email)
        val authentication = UsernamePasswordAuthenticationToken(
            userPrincipal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        return this.mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
    }

    fun createDeviceAuthentication(deviceId: String = "test-device-id"): Authentication {
        val devicePrincipal = DevicePrincipal(deviceId)
        return UsernamePasswordAuthenticationToken(
            devicePrincipal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_DEVICE"))
        )
    }

    fun createUserAuthentication(
        userId: UUID = UUID.randomUUID(),
        email: String = "test@example.com"
    ): Authentication {
        val userPrincipal = UserPrincipal(userId, email)
        return UsernamePasswordAuthenticationToken(
            userPrincipal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }
}