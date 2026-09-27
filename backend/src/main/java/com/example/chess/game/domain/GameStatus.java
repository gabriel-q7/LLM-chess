package com.example.chess.game.domain;

public enum GameStatus {
    PLAYING,
    CHECKMATE,
    STALEMATE,
    DRAW;

    public boolean isFinished() {
        return this != PLAYING;
    }
}
