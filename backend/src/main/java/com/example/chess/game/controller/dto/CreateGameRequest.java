package com.example.chess.game.controller.dto;

import com.example.chess.chess.Color;

/**
 * @param playerColor side the human plays; defaults to White
 */
public record CreateGameRequest(Color playerColor) {

    public Color playerColorOrDefault() {
        return playerColor == null ? Color.WHITE : playerColor;
    }
}
