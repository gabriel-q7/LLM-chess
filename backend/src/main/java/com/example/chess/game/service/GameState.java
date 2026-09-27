package com.example.chess.game.service;

import com.example.chess.chess.ChessPosition;
import com.example.chess.game.domain.Game;
import com.example.chess.game.domain.Move;

import java.util.List;

/**
 * A game together with its move history and the position derived from replaying it.
 */
public record GameState(Game game, List<Move> moves, ChessPosition position) {

    public boolean isPlayersTurn() {
        return !game.getStatus().isFinished() && position.turn() == game.getPlayerColor();
    }

    public String lastMoveSan() {
        return moves.isEmpty() ? null : moves.getLast().getSan();
    }
}
