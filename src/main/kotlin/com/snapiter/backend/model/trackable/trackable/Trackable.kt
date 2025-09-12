package com.snapiter.backend.model.trackable.trackable

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("trackables")
data class Trackable (
    @Id
    val trackableId: String,
    val name: String?,
    val websiteTitle: String = "",
    val website: String = "",
    val hostName: String = "",
    val icon: String = "",

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    val createdAt: LocalDateTime?
)
