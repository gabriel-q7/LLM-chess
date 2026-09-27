package com.example.chess.game.controller.dto;

import com.example.chess.chess.ChessMove;

public record LegalMoveResponse(String from, String to, String promotion) {

    public static LegalMoveResponse from(ChessMove move) {
        return new LegalMoveResponse(move.from(), move.to(), move.promotion());
    }
}
