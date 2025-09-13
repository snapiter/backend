package com.snapiter.backend.model.trackable.trackable

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface TrackableRepository : ReactiveCrudRepository<Trackable, Long> {
    fun findByTrackableId(trackableId: String): Mono<Trackable>
    fun findFirstByHostName(hostName: String): Mono<Trackable>
}
