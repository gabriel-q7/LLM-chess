package com.example.chess.engine;

import java.util.List;

/**
 * The computer's move together with everything the engine was given and returned for it.
 *
 * @param move               chosen move in UCI notation
 * @param strength           skill level and depth requested
 * @param depth              depth the search reached
 * @param evaluation         pawns from White's point of view; ±{@value EngineAnalysis#MATE_SCORE} for forced mates
 * @param mateIn             moves to mate (positive: White mates), or {@code null}
 * @param principalVariation line the engine expected, in UCI notation
 * @param uciCommands        UCI commands sent to Stockfish
 * @param finalInfo          last {@code info} line Stockfish reported
 * @param bestMoveLine       Stockfish's {@code bestmove} line
 */
public record EngineMove(
        String move,
        EngineStrength strength,
        int depth,
        double evaluation,
        Integer mateIn,
        List<String> principalVariation,
        List<String> uciCommands,
        String finalInfo,
        String bestMoveLine
) {

    public EngineMove {
        principalVariation = principalVariation == null ? List.of() : List.copyOf(principalVariation);
        uciCommands = uciCommands == null ? List.of() : List.copyOf(uciCommands);
    }
}
