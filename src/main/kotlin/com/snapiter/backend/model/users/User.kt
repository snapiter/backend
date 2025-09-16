package com.snapiter.backend.model.users

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("users")
data class User(
    @JsonIgnore
    @Id var id: Long?,
    val userId: UUID,
    val email: String,
    val emailVerified: Boolean,
    val displayName: String?,
    val createdAt: OffsetDateTime,
    val lastLoginAt: OffsetDateTime?
)
