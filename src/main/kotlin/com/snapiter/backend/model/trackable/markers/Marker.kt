package com.snapiter.backend.model.trackable.markers

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("markers")
data class Marker (
    @JsonIgnore
    @Id var id: Long?,
    val trackableId: String,
    val markerId: String,
    val fileSize: Number,
    val fileType: String,
    val latitude: Double?,
    val longitude: Double?,
    val title: String = "",
    val description: String = "",
    val hasThumbnail: Boolean = false,

    val createdAt: Instant?
) {
    companion object {
        fun create(trackableId: String, markerId: String, latitude: Double, longitude: Double, fileSize: Number, fileType: String): Marker {
            return Marker(null, trackableId, markerId, fileSize, fileType, latitude, longitude, "","",false, null)
        }
        fun create(trackableId: String, markerId: String, fileSize: Number, fileType: String): Marker {
            return Marker(null, trackableId, markerId, fileSize, fileType, null, null,"","",false, null)
        }
    }
}
