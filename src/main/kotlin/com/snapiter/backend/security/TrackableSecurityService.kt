package com.snapiter.backend.security

import com.snapiter.backend.model.trackable.devices.DeviceRepository
import com.snapiter.backend.model.trackable.trackable.TrackableRepository
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service("trackableAccessChecker")
class TrackableSecurityService(
    private val trackableRepository: TrackableRepository,
    private val deviceRepository: DeviceRepository
) {
    fun canAccess(trackableId: String, auth: Authentication): Mono<Boolean> {
        val principal = auth.principal as? AppPrincipal ?: return Mono.just(false)

        return when (principal) {
            is UserPrincipal -> checkUserAccess(trackableId, principal)
            is DevicePrincipal -> checkDeviceAccess(trackableId, principal)
        }
    }

    private fun checkUserAccess(trackableId: String, user: UserPrincipal): Mono<Boolean> {
        return trackableRepository.existsByTrackableIdAndUserId(trackableId, user.userId)
    }

    private fun checkDeviceAccess(trackableId: String, device: DevicePrincipal): Mono<Boolean> {
        return deviceRepository.existsByTrackableIdAndDeviceId(trackableId, device.deviceId)
    }

}