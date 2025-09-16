package com.snapiter.backend.security

import com.snapiter.backend.model.trackable.trackable.TrackableRepository
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component("trackableMethodSecurity")
class TrackableMethodSecurity(
    private val trackableRepository: TrackableRepository
) {

    fun canAccess(trackableId: String, authentication: Authentication): Mono<Boolean> {
        val principal = authentication.principal as? AppPrincipal ?: return Mono.just(false)

        return when (principal) {
            is UserPrincipal -> checkAccess(trackableId)
            is DevicePrincipal -> checkAccess(trackableId)
        }
    }

    private fun checkAccess(trackableId: String): Mono<Boolean> {
        // For now, just check if trackable exists
        return trackableRepository.findByTrackableId(trackableId)
            .map { true }
            .defaultIfEmpty(false)
    }
}