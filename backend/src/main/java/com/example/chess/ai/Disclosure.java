package com.example.chess.ai;

/**
 * What the assistant may reveal about the engine analysis. Facts that are not disclosed are
 * removed before anything is sent to Laya, so the assistant never sees them.
 *
 * @param bestMove          the exact best move in SAN
 * @param pieceHint         which piece (or castling) the best move uses, without the square
 * @param continuation      the engine's expected line after the best move
 * @param numericEvaluation the score in pawns and the search depth; otherwise only words ("White is better")
 * @param playerMate        how much to say about a forced mate the player has
 */
public record Disclosure(
        boolean bestMove,
        boolean pieceHint,
        boolean continuation,
        boolean numericEvaluation,
        MateDisclosure playerMate
) {

    /** Everything, for learning. */
    public static final Disclosure FULL = new Disclosure(true, false, true, true, MateDisclosure.EXACT);

    /** A nudge: which piece to look at and a verbal assessment, never the move itself. */
    public static final Disclosure HINTS = new Disclosure(false, true, false, false, MateDisclosure.EXISTS);

    /** Only what a player can see on the board: checks, threats against them, a verbal assessment. */
    public static final Disclosure MINIMAL = new Disclosure(false, false, false, false, MateDisclosure.HIDDEN);

    public enum MateDisclosure {
        /** "White can force mate in 3". */
        EXACT,
        /** "White has a forced mate". */
        EXISTS,
        /** Reported only as "White is winning". */
        HIDDEN
    }
}
