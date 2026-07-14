package com.snapiter.backend.api

import com.snapiter.backend.model.trackable.devices.DeviceNotFoundException
import com.snapiter.backend.model.trackable.positionreport.InvalidCoordinateException
import com.snapiter.backend.model.trackable.positionreport.PositionInFutureException
import com.snapiter.backend.model.trackable.devices.tokens.UnauthorizedTokenException
import com.snapiter.backend.model.trackable.devices.tokens.UnclaimedTokenNotFound
import com.snapiter.backend.security.ExpiredTokenException
import com.snapiter.backend.security.InvalidTokenException
import com.snapiter.backend.security.UnauthorizedRefreshTokenException
import com.fasterxml.jackson.annotation.JsonInclude
import com.snapiter.backend.api.trackable.PositionValidationException
import io.jsonwebtoken.ExpiredJwtException
import jakarta.mail.SendFailedException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException
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

    @ExceptionHandler(UnauthorizedRefreshTokenException::class)
    fun invalidTokenException(ex: UnauthorizedRefreshTokenException): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = "unauthorized_" + ex.message,
            message = "You are not authorized"
        )
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse))
    }
    @ExceptionHandler(UnauthorizedTokenException::class)
    fun invalidTokenException(ex: UnauthorizedTokenException): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = "unauthorized_token_" + ex.message,
            message = "You are not authorized"
        )
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse))
    }

    @ExceptionHandler(UnclaimedTokenNotFound::class)
    fun unclaimedTokenException(ex: UnclaimedTokenNotFound): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = ex.message ?: "unclaimed_token_error",
            message = "Could not find unclaimed token"
        )
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse))
    }

    @ExceptionHandler(DeviceNotFoundException::class)
    fun handleDeviceNotFound(ex: DeviceNotFoundException): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = "device_not_found",
            message = ex.message ?: "Device not found"
        )
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse))
    }

    @ExceptionHandler(PositionInFutureException::class)
    fun handlePositionInFuture(ex: PositionInFutureException): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = "position_in_future",
            message = ex.message ?: "createdAt must not be in the future"
        )
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse))
    }

    @ExceptionHandler(InvalidCoordinateException::class)
    fun handleInvalidCoordinate(ex: InvalidCoordinateException): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = "invalid_coordinate",
            message = ex.message ?: "Coordinates are out of range"
        )
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse))
    }

    @ExceptionHandler(ExpiredJwtException::class)
    fun invalidJwtTokenException(ex: ExpiredJwtException): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = "expired_token",
            message = "The token has expired. Please request a new one."
        )
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse))
    }

    @ExceptionHandler(SendFailedException::class)
    fun smtpFailure(ex: SendFailedException): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = "email_broken",
            message = "Could not send an email"
        )
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse))
    }

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidation(ex: WebExchangeBindException): Mono<ResponseEntity<ErrorResponse>> {
        val fields = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") }
        val errorResponse = ErrorResponse(
            error = "validation_error",
            message = "One or more fields are invalid",
            fields = fields
        )
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse))
    }

    @ExceptionHandler(PositionValidationException::class)
    fun handlePositionValidation(ex: PositionValidationException): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = "validation_error",
            message = ex.message ?: "The position created at is incorrect"
        )
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse))
    }

    @ExceptionHandler(ServerWebInputException::class)
    fun handleMalformedInput(ex: ServerWebInputException): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            error = "malformed_request",
            message = "The request body could not be read"
        )
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse))
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): Mono<ResponseEntity<ErrorResponse>> {
        val status = HttpStatus.resolve(ex.statusCode.value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
        val errorResponse = ErrorResponse(
            error = status.name.lowercase(),
            message = ex.reason ?: "Request could not be processed"
        )
        return Mono.just(ResponseEntity.status(ex.statusCode).body(errorResponse))
    }

}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val error: String,
    val message: String,
    val fields: Map<String, String>? = null
)