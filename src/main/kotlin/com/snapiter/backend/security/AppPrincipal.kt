package com.snapiter.backend.security

import java.util.UUID

sealed interface AppPrincipal {
    val userId: UUID
}

data class UserPrincipal(
    override val userId: UUID,
    val email: String
) : AppPrincipal

data class DevicePrincipal(
    override val userId: UUID,
    val deviceId: String
) : AppPrincipal
