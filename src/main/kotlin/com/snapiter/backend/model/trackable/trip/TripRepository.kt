package com.snapiter.backend.model.trackable.trip

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface TripRepository : ReactiveCrudRepository<Trip, Long> {
    @Query("SELECT * FROM trip WHERE trackable_id = :trackableId ORDER BY end_date DESC NULLS FIRST")
    fun findAllByTrackableIdOrderByEndDateDescNullsFirst(trackableId: String): Flux<Trip>
    fun findBySlugAndTrackableId(slug: String, trackableId: String): Mono<Trip>
}
