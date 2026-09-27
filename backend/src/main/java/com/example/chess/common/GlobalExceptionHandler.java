package com.example.chess.common;

import com.example.chess.ai.AiServiceUnavailableException;
import com.example.chess.chess.IllegalMoveException;
import com.example.chess.chess.InvalidPositionException;
import com.example.chess.engine.EngineUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApi(ApiException e) {
        return respond(e.code(), e.getMessage());
    }

    @ExceptionHandler(IllegalMoveException.class)
    ResponseEntity<ApiError> handleIllegalMove(IllegalMoveException e) {
        return respond(ErrorCode.ILLEGAL_MOVE, ErrorCode.ILLEGAL_MOVE.defaultMessage());
    }

    @ExceptionHandler(InvalidPositionException.class)
    ResponseEntity<ApiError> handleInvalidPosition(InvalidPositionException e) {
        log.warn("Invalid position: {}", e.getMessage());
        return respond(ErrorCode.INVALID_POSITION, ErrorCode.INVALID_POSITION.defaultMessage());
    }

    @ExceptionHandler(EngineUnavailableException.class)
    ResponseEntity<ApiError> handleEngine(EngineUnavailableException e) {
        log.error("Chess engine failure", e);
        return respond(ErrorCode.ENGINE_UNAVAILABLE, ErrorCode.ENGINE_UNAVAILABLE.defaultMessage());
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    ResponseEntity<ApiError> handleAi(AiServiceUnavailableException e) {
        log.error("AI service failure", e);
        return respond(ErrorCode.AI_SERVICE_UNAVAILABLE, ErrorCode.AI_SERVICE_UNAVAILABLE.defaultMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> handleConcurrentUpdate(ObjectOptimisticLockingFailureException e) {
        return respond(ErrorCode.CONCURRENT_UPDATE, ErrorCode.CONCURRENT_UPDATE.defaultMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respond(ErrorCode.VALIDATION_ERROR, message.isBlank() ? ErrorCode.VALIDATION_ERROR.defaultMessage() : message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> handleUnreadable(Exception e) {
        return respond(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return respond(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage());
    }

    private static ResponseEntity<ApiError> respond(ErrorCode code, String message) {
        return ResponseEntity.status(code.status()).body(new ApiError(code.name(), message));
    }
}
