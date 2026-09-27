package com.example.chess.engine;

import java.util.List;

/**
 * Engine output normalised to White's point of view.
 *
 * @param bestMove           best move in UCI notation, or {@code null} when the side to move has no legal move
 * @param evaluation         evaluation in pawns; positive favours White. For forced mates it is ±{@value #MATE_SCORE}
 * @param depth              search depth reached
 * @param mateIn             moves to mate (positive: White mates, negative: Black mates), or {@code null}
 * @param principalVariation expected continuation in UCI notation, starting with {@code bestMove}
 */
public record EngineAnalysis(
        String bestMove,
        double evaluation,
        int depth,
        Integer mateIn,
        List<String> principalVariation
) {

    public static final double MATE_SCORE = 100.0;

    public EngineAnalysis {
        principalVariation = principalVariation == null ? List.of() : List.copyOf(principalVariation);
    }
}
