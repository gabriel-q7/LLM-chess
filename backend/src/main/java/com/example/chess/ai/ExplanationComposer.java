package com.example.chess.ai;

import com.example.chess.chess.Color;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static com.example.chess.ai.PromptBuilder.name;

/**
 * Turns engine facts and Laya's decisions into prose. Every sentence states a fact supplied by
 * the rules engine or Stockfish; Laya only chooses which facts to put forward.
 */
@Component
public class ExplanationComposer {

    public String explain(PositionFacts facts, String focus) {
        if (!facts.hasBestMove()) {
            return gameOver(facts);
        }
        StringBuilder text = new StringBuilder();
        if (facts.lastMove() != null) {
            text.append("After ").append(facts.lastMove()).append(", ").append(describeBalance(facts));
        } else {
            text.append(capitalize(describeBalance(facts)));
        }
        text.append(" (")
                .append(formatEvaluation(facts)).append(" at depth ").append(facts.depth()).append("). ");
        text.append(bestMoveSentence(facts));
        if (facts.inCheck()) {
            text.append(' ').append(name(facts.sideToMove())).append(" is in check and must respond to it.");
        }
        text.append(' ').append("Material: ").append(describeMaterial(facts)).append('.');
        String theme = themeSentence(focus);
        if (theme != null) {
            text.append(' ').append(theme);
        }
        return text.toString();
    }

    public String answer(String intent, PositionFacts facts, String focus) {
        if (!facts.hasBestMove()) {
            return gameOver(facts);
        }
        String balance = capitalize(describeBalance(facts)) + " (" + formatEvaluation(facts) + ").";
        return switch (intent == null ? "unknown" : intent) {
            case "best_move" -> bestMoveSentence(facts);
            case "evaluation" -> balance + " Material: " + describeMaterial(facts) + ".";
            case "last_move" -> lastMoveAnswer(facts);
            case "threats" -> threatsAnswer(facts);
            case "plan" -> {
                String theme = themeSentence(focus);
                yield (theme == null ? "" : theme + " ") + bestMoveSentence(facts);
            }
            case "other" -> "I can only talk about this game: ask about the best move, who is better, "
                    + "the last move, threats or plans.";
            default -> "I'm not sure what you are asking. " + balance + " " + bestMoveSentence(facts);
        };
    }

    private String lastMoveAnswer(PositionFacts facts) {
        if (facts.lastMove() == null) {
            return "No moves have been played yet. " + bestMoveSentence(facts);
        }
        return (facts.lastMove() + " was the last move. After it, " + describeBalance(facts) + " ("
                + formatEvaluation(facts) + "). " + continuationSentence(facts)).trim();
    }

    private String threatsAnswer(PositionFacts facts) {
        StringBuilder text = new StringBuilder();
        if (facts.inCheck()) {
            text.append(name(facts.sideToMove())).append(" is in check. ");
        }
        if (facts.mateIn() != null && facts.mateIn() != 0) {
            Color mating = facts.mateIn() > 0 ? Color.WHITE : Color.BLACK;
            text.append(name(mating)).append(" has a forced mate in ").append(Math.abs(facts.mateIn())).append(". ");
        }
        String best = facts.bestMove();
        if (best.contains("x")) {
            text.append("The engine's best move ").append(best).append(" wins or trades material. ");
        }
        if (best.endsWith("+")) {
            text.append("The engine's best move ").append(best).append(" gives check. ");
        }
        if (text.isEmpty()) {
            text.append("Stockfish sees no immediate check, capture or mating threat in its main line. ");
        }
        return text.append(continuationSentence(facts)).toString().trim();
    }

    private String bestMoveSentence(PositionFacts facts) {
        return "Stockfish's best move for " + name(facts.sideToMove()) + " is " + facts.bestMove() + "."
                + (facts.continuation().size() > 1 ? " " + continuationSentence(facts) : "");
    }

    private String continuationSentence(PositionFacts facts) {
        if (facts.continuation().size() <= 1) {
            return "";
        }
        return "The expected continuation is " + String.join(" ", facts.continuation()) + ".";
    }

    private static String themeSentence(String focus) {
        if (focus == null) {
            return null;
        }
        return switch (focus) {
            case "development" -> "The key theme is development: bringing pieces out, castling and controlling the center.";
            case "attack" -> "The key theme is the attack on the king.";
            case "material" -> "The key theme is material: captures and piece trades.";
            case "defense" -> "The key theme is defense: meeting the opponent's threats.";
            case "endgame" -> "The key theme is endgame technique: king activity and passed pawns.";
            default -> null;
        };
    }

    private static String gameOver(PositionFacts facts) {
        if (facts.inCheck()) {
            return "The game is over: " + name(facts.sideToMove()) + " is checkmated.";
        }
        return "The game is over: " + name(facts.sideToMove()) + " has no legal moves.";
    }

    static String formatEvaluation(PositionFacts facts) {
        if (facts.mateIn() != null) {
            return facts.mateIn() >= 0 ? "#" + facts.mateIn() : "#-" + Math.abs(facts.mateIn());
        }
        return String.format(Locale.ROOT, "%+.2f", facts.evaluation());
    }

    static String describeBalance(PositionFacts facts) {
        if (facts.mateIn() != null && facts.mateIn() != 0) {
            Color mating = facts.mateIn() > 0 ? Color.WHITE : Color.BLACK;
            return name(mating) + " can force mate in " + Math.abs(facts.mateIn());
        }
        double eval = facts.evaluation();
        double abs = Math.abs(eval);
        String leader = eval > 0 ? "White" : "Black";
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

    static String describeMaterial(PositionFacts facts) {
        int diff = facts.whiteMaterial() - facts.blackMaterial();
        if (diff == 0) {
            return "equal";
        }
        String leader = diff > 0 ? "White" : "Black";
        int points = Math.abs(diff);
        return leader + " is up " + points + (points == 1 ? " point" : " points");
    }

    private static String capitalize(String text) {
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }
}
