package com.example.chess.ai;

import com.example.chess.chess.ChessService;
import com.example.chess.chess.Color;
import com.example.chess.engine.EngineAnalysis;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Decides what the assistant knows and builds the structured context sent to Laya. Laya answers
 * typed questions over a text "state"; the state holds only disclosed facts, so neither Laya nor
 * the reply can mention the best move, the engine line or the score when the level hides them.
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

    public PositionFacts facts(PositionContext context) {
        String fen = context.fen();
        EngineAnalysis analysis = context.analysis();
        Disclosure disclosure = context.disclosure();
        Color player = context.playerColor();

        List<String> line = analysis.principalVariation().isEmpty() && analysis.bestMove() != null
                ? List.of(analysis.bestMove())
                : analysis.principalVariation();
        List<String> san = chessService.toSanLine(fen, line);
        String bestMove = san.isEmpty() ? null : san.getFirst();

        Integer mateIn = analysis.mateIn() == null || analysis.mateIn() == 0 ? null : analysis.mateIn();
        Color mating = mateIn == null ? null : (mateIn > 0 ? Color.WHITE : Color.BLACK);
        boolean playerMates = mating == player;
        boolean mateVisible = mating != null && (!playerMates || disclosure.playerMate() != Disclosure.MateDisclosure.HIDDEN);

        int white = chessService.material(fen, Color.WHITE);
        int black = chessService.material(fen, Color.BLACK);
        int moveNumber = Integer.parseInt(fen.trim().split("\\s+")[5]);

        return new PositionFacts(
                chessService.sideToMove(fen),
                player,
                moveNumber,
                phase(moveNumber, white + black),
                white,
                black,
                chessService.position(fen, List.of()).check(),
                context.lastMove(),
                balance(analysis.evaluation(), mating, mateIn, playerMates, disclosure),
                disclosure.numericEvaluation() ? formatEvaluation(analysis, mateVisible) + " at depth " + analysis.depth() : null,
                disclosure.bestMove() ? bestMove : null,
                disclosure.pieceHint() && bestMove != null ? pieceOf(bestMove) : null,
                disclosure.continuation() ? san.stream().limit(PV_LENGTH).toList() : List.of(),
                mateIn != null && !playerMates ? Math.abs(mateIn) : null,
                mateIn != null && playerMates && disclosure.playerMate() == Disclosure.MateDisclosure.EXACT ? Math.abs(mateIn) : null,
                mateIn != null && playerMates && disclosure.playerMate() != Disclosure.MateDisclosure.HIDDEN,
                analysis.bestMove() == null);
    }

    public LayaModels.Request positionRequest(PositionFacts facts, String model) {
        return new LayaModels.Request(model, describe(facts), Map.of(FOCUS,
                LayaModels.Question.choice("Which theme best describes what matters most in this position?", FOCUSES)));
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

    /** Plain-English statement of the disclosed facts. */
    String describe(PositionFacts facts) {
        StringBuilder text = new StringBuilder("Facts about the chess position.\n");
        text.append("Move ").append(facts.moveNumber()).append(", ")
                .append(facts.phase().name().toLowerCase(Locale.ROOT)).append(". ")
                .append(name(facts.sideToMove())).append(" to move.\n");
        text.append("Last move: ").append(facts.lastMove() == null ? "none" : facts.lastMove()).append('\n');
        text.append("Material: ").append(describeMaterial(facts)).append('\n');
        if (facts.inCheck()) {
            text.append(name(facts.sideToMove())).append(" is in check.\n");
        }
        text.append("Assessment: ").append(facts.balance()).append('\n');
        if (facts.evaluation() != null) {
            text.append("Evaluation: ").append(facts.evaluation()).append('\n');
        }
        if (facts.bestMove() != null) {
            text.append("Best move: ").append(facts.bestMove()).append(describeMoveKind(facts.bestMove())).append('\n');
        }
        if (facts.pieceHint() != null) {
            text.append("Piece to consider: ").append(facts.pieceHint()).append('\n');
        }
        if (facts.continuation().size() > 1) {
            text.append("Expected continuation: ").append(String.join(" ", facts.continuation())).append('\n');
        }
        return text.toString();
    }

    private static String balance(double evaluation, Color mating, Integer mateIn, boolean playerMates,
                                  Disclosure disclosure) {
        if (mating != null) {
            if (!playerMates || disclosure.playerMate() == Disclosure.MateDisclosure.EXACT) {
                return name(mating) + " can force mate in " + Math.abs(mateIn);
            }
            return disclosure.playerMate() == Disclosure.MateDisclosure.EXISTS
                    ? name(mating) + " has a forced mate"
                    : name(mating) + " is winning";
        }
        double abs = Math.abs(evaluation);
        String leader = evaluation > 0 ? "White" : "Black";
        if (abs < 0.3) {
            return "the position is roughly equal";
        }
        if (abs < 1.0) {
            return leader + " is slightly better";
        }
        if (abs < 2.5) {
            return leader + " is clearly better";
        }
        return leader + " is winning";
    }

    private static String formatEvaluation(EngineAnalysis analysis, boolean mateVisible) {
        if (analysis.mateIn() != null && mateVisible) {
            return analysis.mateIn() >= 0 ? "#" + analysis.mateIn() : "#-" + Math.abs(analysis.mateIn());
        }
        return String.format(Locale.ROOT, "%+.2f", analysis.evaluation());
    }

    static String pieceOf(String san) {
        if (san.startsWith("O-O")) {
            return "castling";
        }
        return switch (san.charAt(0)) {
            case 'N' -> "knight";
            case 'B' -> "bishop";
            case 'R' -> "rook";
            case 'Q' -> "queen";
            case 'K' -> "king";
            default -> "pawn";
        };
    }

    static String describeMaterial(PositionFacts facts) {
        int diff = facts.whiteMaterial() - facts.blackMaterial();
        if (diff == 0) {
            return "equal";
        }
        int points = Math.abs(diff);
        return (diff > 0 ? "White" : "Black") + " is up " + points + (points == 1 ? " point" : " points");
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
        return Collections.unmodifiableMap(map);
    }
}
