package com.snapiter.backend.model.trackable.devices.tokens

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("device_tokens")
data class DeviceToken(
    @Id val id: Long? = null,
    val trackableId: String,
    val deviceId: String? = null,
    val tokenHash: String,
    val createdAt: OffsetDateTime,
    val revokedAt: OffsetDateTime?
)


