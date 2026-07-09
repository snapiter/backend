package com.snapiter.backend.model.trackable.trip

import com.snapiter.backend.api.trackable.CreateTripRequest
import com.snapiter.backend.api.trackable.UpdateTripRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@Service
class TripService(
    private val tripRepository: TripRepository
) {
    fun getTrips(trackableId: String): Flux<Trip> {
        return tripRepository.findAllByTrackableIdOrderByEndDateDescNullsFirst(trackableId)
    }

    fun getTrip(trackableId: String, slug: String): Mono<Trip> {
        return tripRepository.findBySlugAndTrackableId(slug, trackableId)
    }

    fun createTrip(trackableId: String, body: CreateTripRequest): Mono<Trip> {
        return tripRepository.findBySlugAndTrackableId(body.slug, trackableId)
            .flatMap<Trip> {
                Mono.error(ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists for this trackable"))
            }
            .switchIfEmpty(
                Mono.defer {
                    val trip = Trip(
                        id = null,
                        trackableId = trackableId,
                        startDate = body.startDate,
                        endDate = body.endDate,
                        title = body.title,
                        description = body.description,
                        slug = body.slug,
                        positionType = body.positionType,
                        createdAt = Instant.now(),
                        color = normalizeColor(body.color),
                        animationSpeed = body.animationSpeed
                    )
                    tripRepository.save(trip)
                }
            )
    }

    fun updateTrip(trackableId: String, slug: String, body: UpdateTripRequest): Mono<Trip> {
        return findTripOrError(trackableId, slug)
            .flatMap { existing ->
                val updated = existing.copy(
                    title = body.title ?: existing.title,
                    description = body.description ?: existing.description,
                    slug = body.slug ?: existing.slug,
                    startDate = body.startDate ?: existing.startDate,
                    endDate = body.endDate ?: existing.endDate,
                    positionType = body.positionType ?: existing.positionType,
                    color = body.color?.let { normalizeColor(it) } ?: existing.color,
                    animationSpeed = body.animationSpeed ?: existing.animationSpeed,
                )
                tripRepository.save(updated)
            }
    }

    fun markTripActive(trackableId: String, slug: String): Mono<Trip> {
        return findTripOrError(trackableId, slug)
            .flatMap { existing ->
                tripRepository.save(existing.copy(endDate = null))
            }
    }

    fun endTrip(trackableId: String, slug: String): Mono<Trip> {
        return findTripOrError(trackableId, slug)
            .flatMap { existing ->
                tripRepository.save(existing.copy(endDate = existing.endDate ?: Instant.now()))
            }
    }

    fun deleteTrip(trackableId: String, slug: String): Mono<Void> {
        return findTripOrError(trackableId, slug)
            .flatMap { existing ->
                tripRepository.delete(existing)
            }
    }

    private fun findTripOrError(trackableId: String, slug: String): Mono<Trip> =
        tripRepository.findBySlugAndTrackableId(slug, trackableId)
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found")))

    private fun normalizeColor(color: String): String =
        if (color.startsWith("#")) color else "#$color"
}
