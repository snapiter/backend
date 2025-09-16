package com.snapiter.backend.model.users


import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID


// MagicLinkEntity.kt
@Table("magic_links")
data class MagicLinkEntity(
    @Id val id: Long?,
    val email: String,
    val userId: UUID?,
    val tokenHash: String,
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime,
    val usedAt: OffsetDateTime?
)
