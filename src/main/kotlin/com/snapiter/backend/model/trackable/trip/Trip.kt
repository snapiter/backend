package com.snapiter.backend.model.trackable.trip


import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("trip")
data class Trip (
    @JsonIgnore
    @Id var id: Long?,
    val trackableId: String,

    val startDate: Instant,
    val endDate: Instant?,
    val title: String,
    val description: String?,
    val slug: String,
    val positionType: PositionType = PositionType.HOURLY,

    val createdAt: Instant?,
    val color: String = "#648192",
    val animationSpeed: Number = 10000
)

enum class PositionType {
    HOURLY,
    ALL
}