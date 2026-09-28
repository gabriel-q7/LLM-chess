package com.example.chess.engine;

public interface ChessEngine {

    /** Full-strength analysis of the position. */
    EngineAnalysis analyze(String fen);

    /** Move the computer plays in the position at the given strength, with the engine's full output for it. */
    EngineMove getBestMove(String fen, EngineStrength strength);
}
