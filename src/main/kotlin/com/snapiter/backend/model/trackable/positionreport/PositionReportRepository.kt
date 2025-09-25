package com.snapiter.backend.model.trackable.positionreport

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import java.time.LocalDateTime

interface PositionReportRepository : ReactiveCrudRepository<PositionReport, Long> {
    @Query("SELECT * FROM position_report WHERE trackable_id = :trackableId " +
            "ORDER BY created_at DESC OFFSET :offset LIMIT :limit")
    fun findAllByTrackableId(trackableId: String, offset: Int, limit: Int): Flux<PositionReport>

    @Query("SELECT MAX(id) as id, MAX(trackable_id) as trackable_id, DATE_TRUNC('hour', created_at) AS hour, AVG(latitude) AS latitude, AVG(longitude) as longitude, MAX(created_at) as created_at FROM position_report WHERE trackable_id = :trackableId GROUP BY DATE_TRUNC('hour', created_at) " +
            "ORDER BY hour DESC OFFSET :offset LIMIT :limit",
    )
    fun findAllByTrackableIdAndTruncateByHour(trackableId: String, offset: Int, limit: Int): Flux<PositionReport>

    @Query("SELECT id, trackable_id ,latitude,longitude, created_at FROM position_report WHERE trackable_id = :trackableId " +
            "AND created_at BETWEEN :fromDate AND :untilDate " +
            "ORDER BY created_at DESC OFFSET :offset LIMIT :limit"
    )
    fun findAllByTrackableIdAndCreatedAtIsBetweenOrderByCreatedAtDesc(trackableId: String, fromDate : LocalDateTime, untilDate: LocalDateTime, offset: Int, limit: Int): Flux<PositionReport>

    @Query("SELECT MAX(id) as id, MAX(trackable_id) as trackable_id, DATE_TRUNC('hour', created_at) AS hour, AVG(latitude) AS latitude, AVG(longitude) as longitude, MAX(created_at) as created_at FROM position_report " +
            "WHERE trackable_id = :trackableId GROUP BY DATE_TRUNC('hour', created_at) " +
            "HAVING DATE_TRUNC('hour', created_at) BETWEEN :fromDate AND :untilDate " +
            "ORDER BY hour DESC OFFSET :offset LIMIT :limit",
    )
    fun findAllByTrackableIdAndCreatedAtIsBetweenOrderByCreatedAtDescAndTruncatedByHour(trackableId: String, fromDate: LocalDateTime, untilDate: LocalDateTime, offset: Int, limit: Int): Flux<PositionReport>
}

