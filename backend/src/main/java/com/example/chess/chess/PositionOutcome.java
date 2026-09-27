package com.example.chess.chess;

public enum PositionOutcome {
    ONGOING,
    CHECKMATE,
    STALEMATE,
    DRAW_REPETITION,
    DRAW_INSUFFICIENT_MATERIAL,
    DRAW_FIFTY_MOVE_RULE;

    public boolean isFinished() {
        return this != ONGOING;
    }
}
