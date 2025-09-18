package com.snapiter.backend.model.trackable.markers

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

interface MarkerRepository : ReactiveCrudRepository<Marker, Long> {
    @Query("SELECT * FROM markers WHERE trackable_id = :trackableId " +
            "ORDER BY created_at DESC OFFSET :offset LIMIT :limit")
    fun findAllByTrackableId(trackableId: String, offset: Int, limit: Int): Flux<Marker>


    @Query("SELECT * FROM markers WHERE trackable_id = :trackableId " +
            "AND created_at BETWEEN :fromDate AND :untilDate " +
            "ORDER BY created_at"
    )
    fun findAllByTrackableIdAndCreatedAtIsBetweenOrderByCreatedAtDesc(trackableId: String, fromDate: LocalDateTime, untilDate: LocalDateTime): Flux<Marker>


    fun findByMarkerIdAndTrackableId(markerId: String, trackableId: String): Mono<Marker>
    fun deleteByMarkerIdAndTrackableId(markerId: String, trackableId: String): Mono<Long>

    fun findAllByHasThumbnail(hasThumbnail: Boolean): Flux<Marker>

}

