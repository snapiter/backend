package com.snapiter.backend.model.trackable.devices

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("devices")
data class Device (
    @JsonIgnore
    @Id var id: Long?,
    val trackableId: String,
    val deviceId: String,
    val name: String?,

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    val createdAt: LocalDateTime?,

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    var lastReportedAt: LocalDateTime?,
)
