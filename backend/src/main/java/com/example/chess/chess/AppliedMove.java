package com.example.chess.chess;

/**
 * Result of applying a legal move: its notation and the position it produced.
 */
public record AppliedMove(ChessMove move, Color color, int moveNumber, String san, ChessPosition position) {
}
