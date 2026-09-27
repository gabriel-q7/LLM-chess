package com.example.chess.ai;

import org.springframework.stereotype.Component;

import java.util.Locale;

import static com.example.chess.ai.PromptBuilder.describeMaterial;
import static com.example.chess.ai.PromptBuilder.name;

/**
 * Turns the disclosed facts and Laya's decisions into prose. Every sentence states a fact from
 * {@link PositionFacts}; facts the level hides are absent there, so they cannot leak into a reply.
 */
@Component
public class ExplanationComposer {

    static final String NO_HINTS = "No hints at this level: look for your own threats and your opponent's.";

    public String explain(PositionFacts facts, String focus) {
        if (facts.gameOver()) {
            return gameOver(facts);
        }
        StringBuilder text = new StringBuilder();
        if (facts.lastMove() != null) {
            text.append("After ").append(facts.lastMove()).append(", ").append(facts.balance());
        } else {
            text.append(capitalize(facts.balance()));
        }
        text.append(evaluationSuffix(facts)).append('.');
        append(text, moveAdvice(facts));
        append(text, threats(facts));
        append(text, "Material: " + describeMaterial(facts) + ".");
        append(text, themeSentence(focus));
        return text.toString();
    }

    public String answer(String intent, PositionFacts facts, String focus) {
        if (facts.gameOver()) {
            return gameOver(facts);
        }
        String balance = capitalize(facts.balance()) + evaluationSuffix(facts) + ".";
        return switch (intent == null ? "unknown" : intent) {
            case "best_move" -> {
                String advice = moveAdvice(facts);
                yield advice == null ? NO_HINTS : advice;
            }
            case "evaluation" -> balance + " Material: " + describeMaterial(facts) + ".";
            case "last_move" -> lastMoveAnswer(facts);
            case "threats" -> threatsAnswer(facts);
            case "plan" -> join(themeSentence(focus), moveAdvice(facts), balance);
            case "other" -> "I can only talk about this game: ask about the best move, who is better, "
                    + "the last move, threats or plans.";
            default -> join("I'm not sure what you are asking.", balance, moveAdvice(facts));
        };
    }

    private String lastMoveAnswer(PositionFacts facts) {
        if (facts.lastMove() == null) {
            return join("No moves have been played yet.", moveAdvice(facts));
        }
        return join(facts.lastMove() + " was the last move. After it, " + facts.balance() + evaluationSuffix(facts) + ".",
                continuationSentence(facts));
    }

    private String threatsAnswer(PositionFacts facts) {
        String threats = threats(facts);
        StringBuilder text = new StringBuilder(threats == null ? "" : threats);
        if (facts.bestMove() != null) {
            if (facts.bestMove().contains("x")) {
                append(text, "The engine's best move " + facts.bestMove() + " wins or trades material.");
            }
            if (facts.bestMove().endsWith("+")) {
                append(text, "The engine's best move " + facts.bestMove() + " gives check.");
            }
        }
        if (text.isEmpty()) {
            text.append("There is no check or forced mate against you right now.");
        }
        append(text, continuationSentence(facts));
        return text.toString();
    }

    /** Check and mate threats. Mates against the player are always reported; the player's own only if disclosed. */
    private String threats(PositionFacts facts) {
        StringBuilder text = new StringBuilder();
        if (facts.inCheck()) {
            append(text, (facts.playersTurn() ? "You are" : name(facts.sideToMove()) + " is") + " in check and must respond to it.");
        }
        if (facts.opponentMateIn() != null) {
            append(text, "Your opponent threatens a forced mate in " + facts.opponentMateIn() + ".");
        }
        if (facts.playerMateIn() != null) {
            append(text, "You have a forced mate in " + facts.playerMateIn() + ".");
        } else if (facts.playerHasMate()) {
            append(text, "You have a forced mate: look for it!");
        }
        return text.isEmpty() ? null : text.toString();
    }

    /** The exact move when disclosed, otherwise a piece hint, otherwise nothing. */
    private String moveAdvice(PositionFacts facts) {
        String side = facts.playersTurn() ? "you" : name(facts.sideToMove());
        if (facts.bestMove() != null) {
            return join("Stockfish's best move for " + side + " is " + facts.bestMove() + ".", continuationSentence(facts));
        }
        if (facts.pieceHint() == null) {
            return null;
        }
        if (facts.pieceHint().equals("castling")) {
            return "Hint: consider castling.";
        }
        return "Hint: consider a move with " + (facts.playersTurn() ? "your" : side + "'s") + " " + facts.pieceHint() + ".";
    }

    private String continuationSentence(PositionFacts facts) {
        return facts.continuation().size() <= 1
                ? null
                : "The expected continuation is " + String.join(" ", facts.continuation()) + ".";
    }

    private static String evaluationSuffix(PositionFacts facts) {
        return facts.evaluation() == null ? "" : " (" + facts.evaluation() + ")";
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

    private static void append(StringBuilder text, String sentence) {
        if (sentence == null || sentence.isBlank()) {
            return;
        }
        if (!text.isEmpty()) {
            text.append(' ');
        }
        text.append(sentence);
    }

    private static String join(String... sentences) {
        StringBuilder text = new StringBuilder();
        for (String sentence : sentences) {
            append(text, sentence);
        }
        return text.toString();
    }

    private static String capitalize(String text) {
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }
}
