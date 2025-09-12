package com.snapiter.backend.model.trackable.positionreport

import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface PositionReportRepository : ReactiveCrudRepository<PositionReport, Long> {

}

