package com.snapiter.backend.util.thumbnail

import com.snapiter.backend.model.trackable.markers.MarkerRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ThumbnailCron(
    private val markerRepository: MarkerRepository,
    private val thumbnailGeneratorService: ThumbnailGeneratorService,
) {
    @Scheduled(cron = "0 0 * * * ?")
    fun cron() {
        markerRepository.findAllByHasThumbnail(false).flatMap { marker ->
            try {
                ThumbnailSize.entries.forEach { size ->
                    thumbnailGeneratorService.create(
                        marker.markerId,
                        "markers/",
                        marker.fileType,
                        size.width,
                        size.height
                    )
                }

                markerRepository.save(marker.copy(hasThumbnail = true))
            } catch (e: Throwable) {
                if (e is software.amazon.awssdk.services.s3.model.NoSuchKeyException) {
                    markerRepository.delete(marker)
                } else {
                    throw CouldNotGenerateThumbnail(e.message ?: "no message")
                }
            }
        }
        .onErrorResume {
            // Log the error if necessary
            Mono.empty()
        }
        .subscribe();
    }
}
