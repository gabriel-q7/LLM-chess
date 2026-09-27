package com.example.chess.engine;

public interface ChessEngine {

    /** Full-strength analysis of the position. */
    EngineAnalysis analyze(String fen);

    /** Move the computer plays in the position, in UCI notation (e.g. {@code e7e5}). */
    String getBestMove(String fen);
}
