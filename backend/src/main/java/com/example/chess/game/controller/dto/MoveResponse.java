package com.example.chess.game.controller.dto;

import com.example.chess.chess.Color;
import com.example.chess.game.domain.Move;

public record MoveResponse(int ply, int moveNumber, Color color, String from, String to, String promotion,
                           String san, String fen) {

    public static MoveResponse from(Move move) {
        return new MoveResponse(move.getPly(), move.getMoveNumber(), move.getColor(), move.getFromSquare(),
                move.getToSquare(), move.getPromotion(), move.getSan(), move.getFen());
    }
}
