package com.example.chess.chess;

public class InvalidPositionException extends RuntimeException {

    public InvalidPositionException(String message, Throwable cause) {
        super(message, cause);
    }
}
