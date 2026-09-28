package com.example.chess.game.controller.dto;

import com.example.chess.ai.AiTrace;
import com.example.chess.game.service.GameAnalysisService.PositionAnalysis;

/**
 * Fields the game's difficulty hides are {@code null}.
 *
 * @param bestMove    engine's best move in SAN
 * @param evaluation  pawns from White's point of view
 * @param mateIn      moves to forced mate (positive: White mates)
 * @param explanation AI explanation of the engine analysis
 * @param trace       what the AI received and decided
 */
public record AnalysisResponse(String bestMove, Double evaluation, int depth, Integer mateIn, String explanation,
                               AiTrace trace) {

    public static AnalysisResponse from(PositionAnalysis analysis) {
        return new AnalysisResponse(analysis.bestMoveSan(), analysis.evaluation(), analysis.depth(),
                analysis.mateIn(), analysis.text(), analysis.trace());
    }
}
