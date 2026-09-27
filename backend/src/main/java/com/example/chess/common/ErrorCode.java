package com.example.chess.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "Game not found."),
    ILLEGAL_MOVE(HttpStatus.UNPROCESSABLE_CONTENT, "That move is not legal in the current position."),
    NOT_YOUR_TURN(HttpStatus.CONFLICT, "It is not your turn."),
    GAME_ALREADY_FINISHED(HttpStatus.CONFLICT, "The game is already finished."),
    CONCURRENT_UPDATE(HttpStatus.CONFLICT, "The game was updated by another request. Reload and try again."),
    INVALID_POSITION(HttpStatus.BAD_REQUEST, "The chess position is invalid."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "The request is invalid."),
    ENGINE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The chess engine is unavailable. Try again shortly."),
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The AI assistant is unavailable. Try again shortly."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
