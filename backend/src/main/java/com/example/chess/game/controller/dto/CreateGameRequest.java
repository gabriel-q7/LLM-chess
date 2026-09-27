package com.example.chess.game.controller.dto;

import com.example.chess.chess.Color;
import com.example.chess.game.domain.Difficulty;

/**
 * @param playerColor side the human plays; defaults to White
 * @param difficulty  game level; defaults to Medium
 */
public record CreateGameRequest(Color playerColor, Difficulty difficulty) {

    public Color playerColorOrDefault() {
        return playerColor == null ? Color.WHITE : playerColor;
    }

    public Difficulty difficultyOrDefault() {
        return difficulty == null ? Difficulty.MEDIUM : difficulty;
    }
}
