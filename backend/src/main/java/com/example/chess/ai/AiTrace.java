package com.example.chess.ai;

import java.util.List;
import java.util.Map;

/**
 * Everything the assistant received and decided for one reply. It holds only disclosed facts,
 * so it is safe to show at any difficulty.
 *
 * @param disclosure  what the level allowed
 * @param hiddenFacts facts withheld from the assistant by the level
 * @param exchanges   each request sent to Laya with its answers, in order
 */
public record AiTrace(Disclosure disclosure, List<String> hiddenFacts, List<Exchange> exchanges) {

    /**
     * @param state     text Laya read
     * @param questions questions asked, by id
     * @param decisions Laya's answer to each question
     */
    public record Exchange(String model, String state, Map<String, LayaModels.Question> questions,
                           Map<String, Decision> decisions) {
    }

    /**
     * @param probability probability of {@code choice}
     * @param used        whether the probability reached the confidence threshold, so the reply relied on it
     */
    public record Decision(String choice, double probability, boolean used, Map<String, Double> probabilities) {
    }

    static List<String> hiddenFacts(Disclosure disclosure) {
        List<String> hidden = new java.util.ArrayList<>();
        if (!disclosure.bestMove()) {
            hidden.add("best move");
        }
        if (!disclosure.continuation()) {
            hidden.add("engine continuation");
        }
        if (!disclosure.numericEvaluation()) {
            hidden.add("numeric evaluation and depth");
        }
        switch (disclosure.playerMate()) {
            case EXISTS -> hidden.add("distance of the player's forced mate");
            case HIDDEN -> hidden.add("the player's forced mate");
            default -> {
            }
        }
        hidden.add("FEN");
        return List.copyOf(hidden);
    }
}
