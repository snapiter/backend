package com.snapiter.backend.model.trackable.trip


import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("trip")
data class Trip (
    @JsonIgnore
    @Id var id: Long?,
    val trackableId: String,

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    val startDate: LocalDateTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    val endDate: LocalDateTime?,
    val title: String,
    val description: String?,
    val slug: String,
    val positionType: PositionType = PositionType.HOURLY,

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    val createdAt: LocalDateTime?,
    val color: String = "#648192",
    val animationSpeed: Number = 10000
)

enum class PositionType {
    HOURLY,
    ALL
}