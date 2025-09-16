package com.snapiter.backend.model.users

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("refresh_tokens")
data class RefreshTokenEntity(
    @Id val id: Long? = null,
    val userId: UUID,
    val tokenHash: String,
    val issuedAt: OffsetDateTime,
    val expiresAt: OffsetDateTime,
    val revokedAt: OffsetDateTime? = null,
    val replacedBy: String? = null,
    val userAgent: String? = null,
    val ip: String? = null, // store as text; or use PG INET via converter
    val lastUsedAt: OffsetDateTime? = null
)
