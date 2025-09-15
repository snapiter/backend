package com.snapiter.backend.model.trackable.trackable

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("trackables")
data class Trackable (
    @JsonIgnore
    @Id
    val trackableId: UUID? = null,
    val name: String?,
    val websiteTitle: String = "",
    val website: String = "",
    val hostName: String = "",
    val icon: String = "",

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    val createdAt: LocalDateTime?
)
