package com.snapiter.backend.security

import com.snapiter.backend.model.users.MagicLink
import com.snapiter.backend.model.users.MagicLinkRepository
import com.snapiter.backend.model.users.User
import com.snapiter.backend.model.users.UserRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID

@Service
class MagicLinkService(
    private val userRepo: UserRepository,
    private val magicRepo: MagicLinkRepository,
    private val mailSender: JavaMailSender,
    @Value("\${app.email.ttl-minutes}") private val ttlMinutes: Long,
    @Value("\${app.email.frontend-email-url}") private val frontendMagicUrl: String,
    @Value("\${app.email.from}") private val fromEmail: String,
) {
    private fun normalizeEmail(raw: String) = raw.trim().lowercase()

    fun requestLink(rawEmail: String): Mono<Void> {
        val email = normalizeEmail(rawEmail)
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val expires = now.plusMinutes(ttlMinutes)

        val rawToken = SecureRandom().let { sr ->
            val bytes = ByteArray(32); sr.nextBytes(bytes); Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
        val tokenHash = sha256(rawToken)

        return userRepo.findByEmail(email)
            .switchIfEmpty(
                Mono.just(
                    User(null, UUID.randomUUID(), email, false, null, now, null)
                ).flatMap(userRepo::save)
            )
            .flatMap { user ->
                val entity = MagicLink(
                    id = null,
                    email = email,
                    userId = user.userId,
                    tokenHash = tokenHash,
                    createdAt = now,
                    expiresAt = expires,
                    usedAt = null
                )
                magicRepo.save(entity)
                    .then(sendEmail(email, rawToken))
                    .then()
            }
    }

    fun consume(rawToken: String): Mono<User> {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val tokenHash = sha256(rawToken)

        return magicRepo.findByTokenHash(tokenHash)
            .switchIfEmpty(Mono.error(InvalidTokenException("Invalid token")))
            .flatMap { link ->
                if (link.usedAt != null || now.isAfter(link.expiresAt)) {
                    return@flatMap Mono.error(ExpiredTokenException("Expired or used token"))
                }
                // mark used
                val updated = link.copy(usedAt = now)
                magicRepo.save(updated)
                    .flatMap { userRepo.findByUserId(link.userId!!) }
                    .flatMap { user ->
                        userRepo.save(user.copy(emailVerified = true, lastLoginAt = now))
                    }
            }
    }

    private fun sha256(s: String): String {
        val d = MessageDigest.getInstance("SHA-256")
        return Base64.getUrlEncoder().withoutPadding().encodeToString(d.digest(s.toByteArray()))
    }

    private fun sendEmail(email: String, rawToken: String): Mono<Void> = Mono.fromCallable {
        val link = "$frontendMagicUrl?token=$rawToken"
        val msg = SimpleMailMessage().apply {
            from = fromEmail
            setTo(email)
            subject = "Your sign-in link"
            text = """
                Hi,
                
                Click to sign in:
                $link
                
                This link will expire in $ttlMinutes minutes. If you didn’t request it, you can ignore this email.
            """.trimIndent()
        }
        mailSender.send(msg)
    }.then()
}



class InvalidTokenException(
    override val message: String
) : IllegalArgumentException(message)

class ExpiredTokenException(
    override val message: String
) : IllegalArgumentException(message)

