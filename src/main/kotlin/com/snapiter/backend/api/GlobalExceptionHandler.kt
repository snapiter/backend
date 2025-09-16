package com.snapiter.backend.api

import com.snapiter.backend.security.ExpiredTokenException
import com.snapiter.backend.security.InvalidTokenException
import com.snapiter.backend.security.UnauthorizedException
import io.jsonwebtoken.ExpiredJwtException
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
            error = "duplicate_entry",
            message = "A resource with this identifier already exists"
        )
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse))
    }

    @ExceptionHandler(InvalidTokenException::class)
    fun invalidTokenException(ex: InvalidTokenException): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = "invalid_token",
            message = "The magic link token is invalid."
        )
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse))
    }

    @ExceptionHandler(ExpiredTokenException::class)
    fun invalidTokenException(ex: ExpiredTokenException): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = "expired_token",
            message = "The magic link has expired. Please request a new one."
        )
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse))
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun invalidTokenException(ex: UnauthorizedException): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = "unauthorized",
            message = "You are not authorized"
        )
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse))
    }


    @ExceptionHandler(ExpiredJwtException::class)
    fun invalidJwtTokenException(ex: ExpiredJwtException): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = "expired_token",
            message = "The token has expired. Please request a new one."
        )
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse))
    }
}


data class ErrorResponse(
    val error: String,
    val message: String
)