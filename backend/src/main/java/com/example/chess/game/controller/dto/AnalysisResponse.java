package com.example.chess.game.controller.dto;

import com.example.chess.game.service.GameAnalysisService.PositionAnalysis;

/**
 * @param bestMove    engine's best move in SAN
 * @param evaluation  pawns from White's point of view
 * @param mateIn      moves to forced mate (positive: White mates), or {@code null}
 * @param explanation AI explanation of the engine analysis
 */
public record AnalysisResponse(String bestMove, double evaluation, int depth, Integer mateIn, String explanation) {

    public static AnalysisResponse from(PositionAnalysis analysis) {
        return new AnalysisResponse(analysis.bestMoveSan(), analysis.engine().evaluation(),
                analysis.engine().depth(), analysis.engine().mateIn(), analysis.text());
    }
}
