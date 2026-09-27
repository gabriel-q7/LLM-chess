package com.example.chess.game.domain;

import com.example.chess.ai.Disclosure;
import com.example.chess.engine.EngineStrength;

/**
 * Game level. It sets both how well the computer plays and how much the AI assistant reveals:
 * the harder the game, the stronger the opponent and the less help.
 */
public enum Difficulty {
    EASY(new EngineStrength(2, 4), Disclosure.FULL),
    MEDIUM(new EngineStrength(8, 8), Disclosure.HINTS),
    HARD(new EngineStrength(20, 14), Disclosure.MINIMAL);

    private final EngineStrength strength;
    private final Disclosure disclosure;

    Difficulty(EngineStrength strength, Disclosure disclosure) {
        this.strength = strength;
        this.disclosure = disclosure;
    }

    /** Strength of the computer's moves. */
    public EngineStrength strength() {
        return strength;
    }

    /** What the AI assistant may tell the player. */
    public Disclosure disclosure() {
        return disclosure;
    }
}
