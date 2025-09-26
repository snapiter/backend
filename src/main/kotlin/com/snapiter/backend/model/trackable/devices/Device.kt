package com.snapiter.backend.model.trackable.devices

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("devices")
data class Device (
    @JsonIgnore
    @Id var id: Long?,
    val trackableId: String,
    val deviceId: String,
    val name: String?,

    val createdAt: Instant?,
    var lastReportedAt: Instant?,
)
