package com.snapiter.backend.security

import com.snapiter.backend.model.trackable.trackable.TrackableRepository
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service("trackableAccessChecker")
class TrackableSecurityService(
    private val trackableRepository: TrackableRepository
) {

    fun canAccess(trackableId: String, auth: Authentication): Mono<Boolean> {
        val principal = auth.principal as? AppPrincipal ?: return Mono.just(false)

        // For now, allow access based on role - this can be enhanced later with actual ownership checks
        return when (principal) {
            is UserPrincipal -> checkUserAccess(trackableId, principal)
            is DevicePrincipal -> checkDeviceAccess(trackableId, principal)
        }
    }

    private fun checkUserAccess(trackableId: String, user: UserPrincipal): Mono<Boolean> {
        // TODO: Add actual ownership verification once ownership is implemented
        // For now, allow all authenticated users
        return trackableRepository.findByTrackableId(trackableId)
            .map { true }
            .defaultIfEmpty(false)
    }

    private fun checkDeviceAccess(trackableId: String, device: DevicePrincipal): Mono<Boolean> {
        // TODO: Add actual ownership verification once ownership is implemented
        // For now, allow all authenticated devices
        return trackableRepository.findByTrackableId(trackableId)
            .map { true }
            .defaultIfEmpty(false)
    }

}