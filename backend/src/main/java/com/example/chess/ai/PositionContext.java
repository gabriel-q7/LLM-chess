package com.example.chess.ai;

import com.example.chess.chess.Color;
import com.example.chess.engine.EngineAnalysis;

/**
 * Input for the assistant: the position, the engine's full analysis of it, and what of that
 * analysis the player is allowed to learn.
 *
 * @param lastMove    last move in SAN, or {@code null} before the first move
 * @param playerColor side the human plays; threats against them are always reported
 */
public record PositionContext(
        String fen,
        String lastMove,
        EngineAnalysis analysis,
        Color playerColor,
        Disclosure disclosure
) {
}
