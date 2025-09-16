package com.snapiter.backend.model.users

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import java.util.UUID

interface UserRepository : ReactiveCrudRepository<User, Long> {
    fun findByEmail(email: String): Mono<User>
    fun findByUserId(userId: UUID): Mono<User>
}