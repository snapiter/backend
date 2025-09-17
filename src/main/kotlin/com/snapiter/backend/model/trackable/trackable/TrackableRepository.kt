package com.snapiter.backend.model.trackable.trackable

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface TrackableRepository : ReactiveCrudRepository<Trackable, Long> {
    fun findByTrackableId(trackableId: String): Mono<Trackable>
    fun existsByTrackableIdAndUserId(trackableId: String, userId: UUID): Mono<Boolean>
    fun findFirstByHostName(hostName: String): Mono<Trackable>
    fun findAllByUserId(userId: UUID): Flux<Trackable>
}
