package com.snapiter.backend.model.trackable.devices

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("devices")
data class Device (
    @Id var id: Long?,
    val trackableId: String,
    val deviceId: String,

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    val createdAt: LocalDateTime?,

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    val lastReportedAt: LocalDateTime?,
)
