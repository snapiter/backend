package com.snapiter.backend.model.trackable.trackable

import com.snapiter.backend.api.trackable.CreateTrackableRequest
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class TrackableService ( private val trackableRepository: TrackableRepository ){
    fun createTracker(req: CreateTrackableRequest) : Mono<String> {
        val entity = Trackable(
            name = req.name,
            websiteTitle = req.websiteTitle ?: "",
            website = req.website ?: "",
            hostName = req.hostName ?: "",
            icon = req.icon ?: "",
            createdAt = LocalDateTime.now()
        )

        return trackableRepository.save(entity).map { saved ->
            saved.trackableId.toString()
        }
    }

    fun getByTrackableId(trackableId: String): Mono<Trackable> =
        trackableRepository.findByTrackableId(trackableId)

    fun getByHostName(hostName: String): Mono<Trackable> =
        trackableRepository.findFirstByHostName(hostName)
}
