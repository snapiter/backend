package com.snapiter.backend.util.s3

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class FileResponseWrapperService (
    private val s3FileDownload: S3FileDownload
) {
//    fun downloadFile(
//        fileName: String,
//        fileType: String,
//        fileSize: Number,
//        dir: String
//    ) = ResponseEntity.ok()
//        .header(HttpHeaders.CONTENT_TYPE, fileType)
//        .header(HttpHeaders.CONTENT_LENGTH, fileSize.toString())
//        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
//        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
//        .body(
//            s3FileDownload.downloadFileAsFlux(dir + fileName)
//        )

    private fun getExtension(fileType: String): String {
        val extensionMap = mapOf(
            "image/png" to "png",
            "image/jpeg" to "jpg",
            "image/gif" to "gif",
            // Add more mappings as needed
        )

        return extensionMap[fileType] ?: "jpg"
    }

    fun previewFile(
        trackableId: String,
        fileId: String,
        fileType: String
    ) = ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, fileType)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"$trackableId-$fileId." + getExtension(fileType) + "\"")
        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
        .body(
            s3FileDownload.downloadFileAsFlux("$trackableId/$fileId")
        )

}