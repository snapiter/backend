package com.snapiter.backend.configuration

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
    bearerFormat = "JWT"
)
class OpenApiConfig {
    @Bean
    fun sortTags(): OpenApiCustomizer = OpenApiCustomizer { openApi: OpenAPI ->
        openApi.tags = openApi.tags?.sortedBy { it.name }
    }
}
