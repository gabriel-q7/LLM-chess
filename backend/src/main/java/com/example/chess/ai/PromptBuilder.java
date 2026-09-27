package com.example.chess.ai;

import com.example.chess.chess.ChessService;
import com.example.chess.chess.Color;
import com.example.chess.engine.EngineAnalysis;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the structured context sent to Laya. Laya answers typed questions over a text "state",
 * so the prompt is a plain-English statement of engine facts plus a set of decision questions.
 */
@Component
public class PromptBuilder {

    public static final String INTENT = "intent";
    public static final String FOCUS = "focus";

    private static final int PV_LENGTH = 4;

    private static final Map<String, String> INTENTS = orderedMap(
            "best_move", "which move to play next, a suggestion or hint",
            "evaluation", "who is winning, the score or advantage",
            "last_move", "why a move was played, the reason behind the previous move",
            "threats", "threats, danger, attacks or checks",
            "plan", "strategy, plans and ideas for the coming moves",
            "other", "anything not about this chess game");

    private static final Map<String, String> FOCUSES = orderedMap(
            "development", "opening: develop pieces, castle, control the center",
            "attack", "attack on the king, checks, mating threats",
            "material", "winning or losing material, captures",
            "defense", "defending, escaping check, parrying threats",
            "endgame", "simplified endgame, king activity, passed pawns");

    private final ChessService chessService;

    public PromptBuilder(ChessService chessService) {
        this.chessService = chessService;
    }

    public PositionFacts facts(String fen, String lastMove, EngineAnalysis analysis) {
        List<String> line = analysis.principalVariation().isEmpty() && analysis.bestMove() != null
                ? List.of(analysis.bestMove())
                : analysis.principalVariation();
        List<String> san = chessService.toSanLine(fen, line);
        int white = chessService.material(fen, Color.WHITE);
        int black = chessService.material(fen, Color.BLACK);
        int moveNumber = Integer.parseInt(fen.trim().split("\\s+")[5]);
        return new PositionFacts(
                fen,
                chessService.sideToMove(fen),
                moveNumber,
                phase(moveNumber, white + black),
                white,
                black,
                chessService.position(fen, List.of()).check(),
                lastMove,
                san.isEmpty() ? null : san.getFirst(),
                san.stream().limit(PV_LENGTH).toList(),
                analysis.evaluation(),
                analysis.mateIn(),
                analysis.depth());
    }

    public LayaModels.Request positionRequest(PositionFacts facts, String model) {
        return new LayaModels.Request(model, describe(facts), Map.of(FOCUS, focusQuestion()));
    }

    /**
     * Classifies the player's question on its own: with the engine facts in the same state, Laya
     * tends to read the facts instead of the question.
     */
    public LayaModels.Request intentRequest(String question, String model) {
        String state = "A chess player asks: " + question.trim();
        return new LayaModels.Request(model, state,
                Map.of(INTENT, LayaModels.Question.choice("What is the player asking about?", INTENTS)));
    }

    /** Plain-English statement of the facts. Laya classifies text, so FEN alone tells it little. */
    String describe(PositionFacts facts) {
        StringBuilder text = new StringBuilder("Engine facts about the chess position.\n");
        text.append("FEN: ").append(facts.fen()).append('\n');
        text.append("Move ").append(facts.moveNumber()).append(", ")
                .append(facts.phase().name().toLowerCase(Locale.ROOT)).append(". ")
                .append(name(facts.sideToMove())).append(" to move.\n");
        text.append("Last move: ").append(facts.lastMove() == null ? "none" : facts.lastMove()).append('\n');
        text.append("Material: ").append(ExplanationComposer.describeMaterial(facts)).append('\n');
        if (facts.inCheck()) {
            text.append(name(facts.sideToMove())).append(" is in check.\n");
        }
        text.append("Stockfish analysis:\n");
        text.append("Best move: ").append(facts.hasBestMove() ? facts.bestMove() + describeMoveKind(facts.bestMove()) : "none").append('\n');
        text.append("Evaluation: ").append(ExplanationComposer.formatEvaluation(facts))
                .append(" (").append(ExplanationComposer.describeBalance(facts)).append(")\n");
        text.append("Search depth: ").append(facts.depth()).append('\n');
        if (facts.continuation().size() > 1) {
            text.append("Expected continuation: ").append(String.join(" ", facts.continuation())).append('\n');
        }
        return text.toString();
    }

    private static LayaModels.Question focusQuestion() {
        return LayaModels.Question.choice("Which theme best describes what matters most in this position?", FOCUSES);
    }

    private static String describeMoveKind(String san) {
        StringBuilder kind = new StringBuilder();
        if (san.startsWith("O-O")) {
            kind.append(", castling");
        } else if (san.contains("x")) {
            kind.append(", a capture");
        }
        if (san.contains("=")) {
            kind.append(", a promotion");
        }
        if (san.endsWith("#")) {
            kind.append(", checkmate");
        } else if (san.endsWith("+")) {
            kind.append(", giving check");
        }
        return kind.toString();
    }

    private static PositionFacts.Phase phase(int moveNumber, int totalMaterial) {
        if (totalMaterial <= 30) {
            return PositionFacts.Phase.ENDGAME;
        }
        return moveNumber <= 10 ? PositionFacts.Phase.OPENING : PositionFacts.Phase.MIDDLEGAME;
    }

    static String name(Color color) {
        return color == Color.WHITE ? "White" : "Black";
    }

    private static Map<String, String> orderedMap(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return java.util.Collections.unmodifiableMap(map);
    }
}
