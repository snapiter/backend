package com.snapiter.backend.model.trackable.positionreport

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Table("position_report")
data class PositionReport (
    @JsonIgnore
    @Id var id: Long?,
    val trackableId: String,
    val latitude: Double,
    val longitude: Double,

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    val createdAt: LocalDateTime?
) {
    companion object {
        fun createFromLatAndLong(trackableId: String, latitude: Double, longitude: Double): PositionReport {
            return PositionReport(null, trackableId, latitude, longitude, null)
        }
        fun createFromLatAndLong(trackableId: String, latitude: Double, longitude: Double, createdAt: OffsetDateTime): PositionReport {
            return PositionReport(null, trackableId, latitude, longitude, createdAt.toLocalDateTime())
        }
    }
}
