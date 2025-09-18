package com.snapiter.backend.model.trackable.trackable

import com.snapiter.backend.api.trackable.CreateTrackableRequest
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

@Service
class TrackableService ( private val trackableRepository: TrackableRepository ){
    fun createTracker(req: CreateTrackableRequest, userId: UUID) : Mono<Trackable> {
        val entity = Trackable(
            trackableId = UUID.randomUUID().toString(),
            name = req.name,
            title = req.title ?: "",
            hostName = req.hostName ?: "",
            icon = req.icon ?: "",
            createdAt = LocalDateTime.now(),
            userId = userId
        )

        return trackableRepository.save(entity)
    }

    fun getByTrackableId(trackableId: String): Mono<Trackable> =
        trackableRepository.findByTrackableId(trackableId)

    fun getByHostName(hostName: String): Mono<Trackable> =
        trackableRepository.findFirstByHostName(hostName)

    fun findAllByUserId(userId: UUID): Flux<Trackable> =
        trackableRepository.findAllByUserId(userId)

}
