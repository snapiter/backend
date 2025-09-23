package com.snapiter.backend.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import software.amazon.awssdk.regions.Region

@ConfigurationProperties(prefix = "aws.s3")
data class S3ClientConfigurationProperties(
    val region: Region = Region.EU_WEST_1,
    val bucket: String = "test",
    val endpoint: String = "",
    val forcePathStyle: Boolean = false,
    val accessKeyId: String = "",
    val secretAccessKey: String = "",
    val filesDir: String = "",
    val multipartMinPartSize: Int = 5 * 1024 * 1024
)

