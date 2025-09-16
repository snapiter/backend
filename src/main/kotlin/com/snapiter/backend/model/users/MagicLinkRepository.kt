package com.snapiter.backend.model.users

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface MagicLinkRepository : ReactiveCrudRepository<MagicLink, Long> {
    fun findByTokenHash(tokenHash: String): Mono<MagicLink>
}