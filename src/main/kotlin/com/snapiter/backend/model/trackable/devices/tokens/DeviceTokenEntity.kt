package com.snapiter.backend.model.trackable.devices.tokens

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("device_tokens")
data class DeviceTokenEntity(
    @Id val id: Long? = null,
    val deviceId: String,
    val tokenHash: String,
    val createdAt: OffsetDateTime,
    val revokedAt: OffsetDateTime?
)
