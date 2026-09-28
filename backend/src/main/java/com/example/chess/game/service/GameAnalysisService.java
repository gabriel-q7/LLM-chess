package com.example.chess.game.service;

import com.example.chess.ai.AiReply;
import com.example.chess.ai.AiService;
import com.example.chess.ai.AiTrace;
import com.example.chess.ai.Disclosure;
import com.example.chess.ai.PositionContext;
import com.example.chess.chess.ChessService;
import com.example.chess.engine.ChessEngine;
import com.example.chess.engine.EngineAnalysis;
import com.example.chess.game.domain.Game;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Engine analysis and AI explanations for a game's current position. Stockfish evaluates the
 * position at full strength; the game's difficulty decides how much of that the player may see,
 * both in the AI's words and in the numbers returned.
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
        PositionContext context = context(gameService.getGame(gameId));
        return disclosed(context, aiService.explainPosition(context));
    }

    public PositionAnalysis answer(UUID gameId, String question) {
        PositionContext context = context(gameService.getGame(gameId));
        return disclosed(context, aiService.answerQuestion(question, context));
    }

    private PositionContext context(GameState state) {
        Game game = state.game();
        EngineAnalysis analysis = engine.analyze(game.getFen());
        return new PositionContext(game.getFen(), state.lastMoveSan(), analysis, game.getPlayerColor(),
                game.getDifficulty().disclosure());
    }

    private PositionAnalysis disclosed(PositionContext context, AiReply reply) {
        Disclosure disclosure = context.disclosure();
        EngineAnalysis analysis = context.analysis();
        String bestMove = disclosure.bestMove() ? bestMoveSan(context.fen(), analysis) : null;
        return disclosure.numericEvaluation()
                ? new PositionAnalysis(bestMove, analysis.evaluation(), analysis.mateIn(), analysis.depth(), reply.text(), reply.trace())
                : new PositionAnalysis(bestMove, null, null, analysis.depth(), reply.text(), reply.trace());
    }

    private String bestMoveSan(String fen, EngineAnalysis analysis) {
        if (analysis.bestMove() == null) {
            return null;
        }
        List<String> san = chessService.toSanLine(fen, List.of(analysis.bestMove()));
        return san.isEmpty() ? null : san.getFirst();
    }

    /**
     * Analysis as the player may see it: fields the difficulty hides are {@code null}.
     *
     * @param bestMoveSan best move in SAN
     * @param evaluation  pawns from White's point of view
     * @param mateIn      moves to forced mate (positive: White mates)
     * @param text        the AI's explanation or answer
     * @param trace       what the AI received and decided (disclosed facts only)
     */
    public record PositionAnalysis(String bestMoveSan, Double evaluation, Integer mateIn, int depth, String text,
                                   AiTrace trace) {
    }
}
