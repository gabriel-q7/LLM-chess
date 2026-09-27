package com.example.chess.ai;

import com.example.chess.chess.Color;

import java.util.List;

/**
 * Everything the assistant is allowed to say about a position, derived from the rules engine
 * and Stockfish. Replies are built only from these facts.
 */
public record PositionFacts(
        String fen,
        Color sideToMove,
        int moveNumber,
        Phase phase,
        int whiteMaterial,
        int blackMaterial,
        boolean inCheck,
        String lastMove,
        String bestMove,
        List<String> continuation,
        double evaluation,
        Integer mateIn,
        int depth
) {

    public enum Phase {
        OPENING, MIDDLEGAME, ENDGAME
    }

    public boolean hasBestMove() {
        return bestMove != null;
    }
}
