package com.example.chess.game.controller.dto;

import com.example.chess.chess.Color;
import com.example.chess.chess.PositionOutcome;
import com.example.chess.game.domain.GameStatus;
import com.example.chess.game.service.GameState;

import java.util.List;
import java.util.UUID;

/**
 * @param legalMoves the player's legal moves, computed by the backend; empty when it is not their turn
 * @param winner     winning side after checkmate, otherwise {@code null}
 * @param drawReason REPETITION, INSUFFICIENT_MATERIAL, FIFTY_MOVE_RULE or STALEMATE when drawn, otherwise {@code null}
 */
public record GameResponse(
        UUID id,
        String fen,
        GameStatus status,
        Color turn,
        Color playerColor,
        boolean check,
        Color winner,
        String drawReason,
        List<MoveResponse> moves,
        List<LegalMoveResponse> legalMoves
) {

    public static GameResponse from(GameState state) {
        GameStatus status = state.game().getStatus();
        Color turn = state.position().turn();
        return new GameResponse(
                state.game().getId(),
                state.game().getFen(),
                status,
                turn,
                state.game().getPlayerColor(),
                state.position().check(),
                status == GameStatus.CHECKMATE ? turn.opposite() : null,
                drawReason(state.position().outcome()),
                state.moves().stream().map(MoveResponse::from).toList(),
                state.isPlayersTurn()
                        ? state.position().legalMoves().stream().map(LegalMoveResponse::from).toList()
                        : List.of());
    }

    private static String drawReason(PositionOutcome outcome) {
        return switch (outcome) {
            case STALEMATE -> "STALEMATE";
            case DRAW_REPETITION -> "REPETITION";
            case DRAW_INSUFFICIENT_MATERIAL -> "INSUFFICIENT_MATERIAL";
            case DRAW_FIFTY_MOVE_RULE -> "FIFTY_MOVE_RULE";
            default -> null;
        };
    }
}
