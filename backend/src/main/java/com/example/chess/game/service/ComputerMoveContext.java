package com.example.chess.game.service;

import com.example.chess.engine.EngineMove;
import com.example.chess.game.domain.Difficulty;

import java.util.List;

/**
 * What Stockfish was given and returned for one of the computer's moves. Stored with the move so
 * it can be inspected later; it is not limited by the difficulty's disclosure rules.
 *
 * @param ply                    half-move index of the computer's move
 * @param san                    the move played, in SAN
 * @param fen                    position sent to Stockfish
 * @param principalVariationSan  the engine's expected line, in SAN
 */
public record ComputerMoveContext(
        int ply,
        String san,
        String fen,
        Difficulty difficulty,
        EngineMove engine,
        List<String> principalVariationSan
) {
}
