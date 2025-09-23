package com.snapiter.backend.api

import com.snapiter.backend.util.thumbnail.ThumbnailCron
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/cron")
class CronController(
    private val thumbnailCron: ThumbnailCron,
) {
    @GetMapping("/thumbnail")
    fun runStaticMap() {
        return thumbnailCron.cron()
    }
}