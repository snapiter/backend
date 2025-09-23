package com.snapiter.backend.configuration

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.utils.StringUtils
import java.net.URI
import java.time.Duration

@Configuration
@EnableConfigurationProperties(S3ClientConfigurationProperties::class)
class S3ClientConfiguration {
    @Bean
    fun s3client(
        s3props: S3ClientConfigurationProperties,
        credentialsProvider: AwsCredentialsProvider
    ): S3AsyncClient {
        println(">>> Using S3 endpoint: ${s3props.endpoint}")

        val httpClient = NettyNioAsyncHttpClient.builder()
            .writeTimeout(Duration.ZERO)
            .maxConcurrency(64)
            .build();
        val serviceConfiguration = S3Configuration.builder()
            .checksumValidationEnabled(false)
            .chunkedEncodingEnabled(true)
            .build();
        val b = S3AsyncClient.builder().httpClient(httpClient)
            .region(s3props.region)
            .credentialsProvider(credentialsProvider)
            .serviceConfiguration(serviceConfiguration)
            .forcePathStyle(s3props.forcePathStyle)
            .endpointOverride(URI.create(s3props.endpoint));

        return b.build();
    }

    @Bean
    fun awsCredentialsProvider(s3props: S3ClientConfigurationProperties): AwsCredentialsProvider {
        return if (StringUtils.isBlank(s3props.accessKeyId)) {
            DefaultCredentialsProvider.create()
        } else {
            AwsCredentialsProvider {
                val creds: AwsCredentials =
                    AwsBasicCredentials.create(s3props.accessKeyId, s3props.secretAccessKey)
                creds
            }
        }
    }
}
