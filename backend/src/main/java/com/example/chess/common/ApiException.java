package com.example.chess.common;

/**
 * Exception carrying a stable error code. Its message is shown to API clients, so it must never
 * contain infrastructure details.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code) {
        this(code, code.defaultMessage());
    }

    public ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
