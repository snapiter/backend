package com.snapiter.backend.model.users

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import java.util.UUID

interface UserRepository : ReactiveCrudRepository<UserEntity, Long> {
    fun findByEmail(email: String): Mono<UserEntity>
    fun findByUserId(userId: UUID): Mono<UserEntity>
}