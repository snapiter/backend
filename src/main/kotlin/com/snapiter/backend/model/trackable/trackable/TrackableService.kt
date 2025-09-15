package com.snapiter.backend.model.trackable.trackable

import com.snapiter.backend.api.trackable.CreateTrackableRequest
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

@Service
class TrackableService ( private val trackableRepository: TrackableRepository ){
    fun createTracker(req: CreateTrackableRequest) : Mono<String> {
        val entity = Trackable(
            trackableId = UUID.randomUUID().toString(),
            name = req.name,
            websiteTitle = req.websiteTitle ?: "",
            website = req.website ?: "",
            hostName = req.hostName ?: "",
            icon = req.icon ?: "",
            createdAt = LocalDateTime.now()
        )

        return trackableRepository.save(entity).map { saved ->
            saved.trackableId
        }
    }

    fun getByTrackableId(trackableId: String): Mono<Trackable> =
        trackableRepository.findByTrackableId(trackableId)

    fun getByHostName(hostName: String): Mono<Trackable> =
        trackableRepository.findFirstByHostName(hostName)
}
