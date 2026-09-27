package com.example.chess.chess;

import java.util.List;

/**
 * Immutable snapshot of a position after replaying a game.
 */
public record ChessPosition(
        String fen,
        Color turn,
        boolean check,
        PositionOutcome outcome,
        List<ChessMove> legalMoves
) {
}
