package com.example.chess.engine;

/**
 * How strongly the engine plays a move.
 *
 * @param skillLevel Stockfish "Skill Level", 0 (weakest) to 20 (full strength)
 * @param depth      search depth in plies
 */
public record EngineStrength(int skillLevel, int depth) {

    public EngineStrength {
        if (skillLevel < 0 || skillLevel > 20) {
            throw new IllegalArgumentException("Skill level must be between 0 and 20: " + skillLevel);
        }
        if (depth < 1) {
            throw new IllegalArgumentException("Depth must be positive: " + depth);
        }
    }
}
