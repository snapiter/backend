package com.snapiter.backend.model.trackable.trip

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface TripRepository : ReactiveCrudRepository<Trip, Long> {
    fun findAllByTrackableId(trackableId: String): Flux<Trip>
    fun findBySlugAndTrackableId(slug: String, trackableId: String): Mono<Trip>
}
