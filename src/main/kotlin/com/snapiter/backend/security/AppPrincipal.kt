package com.snapiter.backend.security

import java.util.UUID

sealed interface AppPrincipal
data class UserPrincipal(val userId: UUID, val email: String): AppPrincipal
data class DevicePrincipal(val deviceId: String): AppPrincipal
