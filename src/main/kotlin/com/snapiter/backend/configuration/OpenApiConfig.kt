package com.snapiter.backend.configuration

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.context.annotation.Configuration

import org.springdoc.core.customizers.OpenApiCustomizer
import io.swagger.v3.oas.models.OpenAPI
import org.springframework.context.annotation.Bean

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "User access token. Send: Authorization: Bearer <jwt>"
)
@SecurityScheme(
    name = "deviceToken",
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.HEADER,
    paramName = "X-Device-Token",
    description = "Opaque device token. Send header: X-Device-Token: <token>"
)
class OpenApiConfig {
    @Bean
    fun sortTags(): OpenApiCustomizer = OpenApiCustomizer { openApi: OpenAPI ->
        openApi.tags = openApi.tags?.sortedBy { it.name }
    }
}
