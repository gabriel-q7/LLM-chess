package com.example.chess.ai;

import com.example.chess.engine.EngineAnalysis;

/**
 * Natural-language assistant. Implementations explain engine output; they never decide move
 * legality or calculate moves themselves.
 */
public interface AiService {

    String explainPosition(String fen, String lastMove, EngineAnalysis analysis);

    String answerQuestion(String question, String fen, String lastMove, EngineAnalysis analysis);
}
