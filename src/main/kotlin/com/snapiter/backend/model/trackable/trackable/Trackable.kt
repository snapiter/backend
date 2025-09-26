package com.snapiter.backend.model.trackable.trackable

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("trackables")
data class Trackable (
    @JsonIgnore
    @Id
    val id: Long? = null,

    val trackableId: String?,
    val name: String?,
    val title: String = "",
    val hostName: String = "",
    val icon: String = "",

    val createdAt: Instant?,

    @JsonIgnore
    val userId: UUID
)
