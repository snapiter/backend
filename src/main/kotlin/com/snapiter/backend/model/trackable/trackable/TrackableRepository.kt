package com.snapiter.backend.model.trackable.trackable

import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface TrackableRepository : ReactiveCrudRepository<Trackable, Long> {

}
