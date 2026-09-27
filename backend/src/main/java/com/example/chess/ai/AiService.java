package com.example.chess.ai;

/**
 * Natural-language assistant. Implementations explain engine output within the limits of the
 * context's {@link Disclosure}; they never decide move legality or calculate moves themselves.
 */
public interface AiService {

    String explainPosition(PositionContext context);

    String answerQuestion(String question, PositionContext context);
}
