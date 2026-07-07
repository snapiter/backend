package com.snapiter.backend.model.trackable.devices

import com.snapiter.backend.model.trackable.trackable.TrackableService
import com.snapiter.backend.model.users.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class DeviceAlertService(
    private val trackableService: TrackableService,
    private val deviceService: DeviceService,
    private val userRepository: UserRepository,
    private val mailSender: JavaMailSender,
    @Value("\${app.email.from}") private val fromEmail: String,
) {
    /**
     * Sends an alert email to the owner of the given trackable.
     */
    fun alert(trackableId: String, deviceId: String): Mono<Void> {
        val owner = trackableService.getByTrackableId(trackableId)
            .flatMap { trackable -> userRepository.findByUserId(trackable.userId) }

        return Mono.zip(owner, deviceService.getDevice(trackableId, deviceId))
            .flatMap { tuple ->
                val user = tuple.t1
                val device = tuple.t2
                sendEmail(user.email, device.name ?: device.deviceId)
            }
    }

    private fun sendEmail(email: String, deviceName: String): Mono<Void> = Mono.fromCallable {
        val msg = SimpleMailMessage().apply {
            from = fromEmail
            setTo(email)
            subject = "Device battery alert"
            text = """
                Your device "$deviceName" reported a battery alert.
            """.trimIndent()
        }
        mailSender.send(msg)
    }.then()
}
