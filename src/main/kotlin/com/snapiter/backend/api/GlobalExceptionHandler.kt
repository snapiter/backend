package com.snapiter.backend.api

import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import reactor.core.publisher.Mono

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicateKey(ex: DuplicateKeyException): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = "Duplicate entry",
            message = "A resource with this identifier already exists"
        )
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse))
    }
}

data class ErrorResponse(
    val error: String,
    val message: String
)