package com.example.chess.game.controller.dto;

import com.example.chess.ai.AiTrace;
import com.example.chess.game.service.GameAnalysisService.PositionAnalysis;

public record ChatResponse(String answer, AiTrace trace) {

    public static ChatResponse from(PositionAnalysis analysis) {
        return new ChatResponse(analysis.text(), analysis.trace());
    }
}
