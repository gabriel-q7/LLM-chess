package com.example.chess.game.service;

import com.example.chess.ai.AiService;
import com.example.chess.chess.ChessService;
import com.example.chess.engine.ChessEngine;
import com.example.chess.engine.EngineAnalysis;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Engine analysis and AI explanations for a game's current position. Stockfish evaluates the
 * position; the AI service only explains that evaluation.
 */
@Service
public class GameAnalysisService {

    private final GameService gameService;
    private final ChessEngine engine;
    private final AiService aiService;
    private final ChessService chessService;

    public GameAnalysisService(GameService gameService, ChessEngine engine, AiService aiService,
                               ChessService chessService) {
        this.gameService = gameService;
        this.engine = engine;
        this.aiService = aiService;
        this.chessService = chessService;
    }

    public PositionAnalysis analyze(UUID gameId) {
        GameState state = gameService.getGame(gameId);
        String fen = state.game().getFen();
        EngineAnalysis analysis = engine.analyze(fen);
        String explanation = aiService.explainPosition(fen, state.lastMoveSan(), analysis);
        return new PositionAnalysis(bestMoveSan(fen, analysis), analysis, explanation);
    }

    public PositionAnalysis answer(UUID gameId, String question) {
        GameState state = gameService.getGame(gameId);
        String fen = state.game().getFen();
        EngineAnalysis analysis = engine.analyze(fen);
        String answer = aiService.answerQuestion(question, fen, state.lastMoveSan(), analysis);
        return new PositionAnalysis(bestMoveSan(fen, analysis), analysis, answer);
    }

    private String bestMoveSan(String fen, EngineAnalysis analysis) {
        if (analysis.bestMove() == null) {
            return null;
        }
        List<String> san = chessService.toSanLine(fen, List.of(analysis.bestMove()));
        return san.isEmpty() ? null : san.getFirst();
    }

    /**
     * @param bestMoveSan best move in SAN, or {@code null} when the side to move has no legal move
     * @param text        the AI's explanation or answer
     */
    public record PositionAnalysis(String bestMoveSan, EngineAnalysis engine, String text) {
    }
}
