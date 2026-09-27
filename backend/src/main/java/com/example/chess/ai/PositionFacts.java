package com.example.chess.ai;

import com.example.chess.chess.Color;

import java.util.List;

/**
 * The facts the assistant may use for one reply, already filtered by the {@link Disclosure}.
 * Undisclosed facts are {@code null} (or empty) and appear nowhere in the prompt or the reply.
 *
 * @param balance          verbal assessment, e.g. "White is slightly better"
 * @param evaluation       numeric score with depth, e.g. "+0.36 at depth 18", or {@code null}
 * @param bestMove         best move in SAN, or {@code null}
 * @param pieceHint        piece the best move uses ("knight", "castling", …), or {@code null}
 * @param continuation     expected line starting with the best move, or empty
 * @param opponentMateIn   moves until the opponent mates the player, or {@code null}
 * @param playerMateIn     moves until the player mates, or {@code null} (only with an EXACT disclosure)
 * @param playerHasMate    whether the player has a forced mate (EXACT and EXISTS disclosures only)
 * @param gameOver         the side to move has no legal move
 */
public record PositionFacts(
        Color sideToMove,
        Color playerColor,
        int moveNumber,
        Phase phase,
        int whiteMaterial,
        int blackMaterial,
        boolean inCheck,
        String lastMove,
        String balance,
        String evaluation,
        String bestMove,
        String pieceHint,
        List<String> continuation,
        Integer opponentMateIn,
        Integer playerMateIn,
        boolean playerHasMate,
        boolean gameOver
) {

    public enum Phase {
        OPENING, MIDDLEGAME, ENDGAME
    }

    public boolean playersTurn() {
        return sideToMove == playerColor;
    }
}
