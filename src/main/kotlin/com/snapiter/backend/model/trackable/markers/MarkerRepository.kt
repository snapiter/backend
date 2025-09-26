package com.snapiter.backend.model.trackable.markers

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

interface MarkerRepository : ReactiveCrudRepository<Marker, Long> {
    @Query("SELECT * FROM markers WHERE trackable_id = :trackableId " +
            "AND created_at BETWEEN :fromDate AND :untilDate " +
            "ORDER BY created_at DESC"
    )
    fun findAllByTrackableIdAndCreatedAtIsBetweenOrderByCreatedAtDesc(trackableId: String, fromDate: Instant, untilDate: Instant): Flux<Marker>


    fun findByMarkerIdAndTrackableId(markerId: String, trackableId: String): Mono<Marker>

    fun findAllByHasThumbnail(hasThumbnail: Boolean): Flux<Marker>
}

