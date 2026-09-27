package com.example.chess.game.controller.dto;

import com.example.chess.game.service.GameAnalysisService.PositionAnalysis;

public record ChatResponse(String answer, String bestMove, double evaluation, int depth, Integer mateIn) {

    public static ChatResponse from(PositionAnalysis analysis) {
        return new ChatResponse(analysis.text(), analysis.bestMoveSan(), analysis.engine().evaluation(),
                analysis.engine().depth(), analysis.engine().mateIn());
    }
}
