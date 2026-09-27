package com.example.chess.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.Map;

/**
 * Wire types for Laya's {@code POST /v1/systemone} protocol.
 */
public final class LayaModels {

    private LayaModels() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(String model, String state, Map<String, Question> questions) {
    }

    /**
     * @param type     {@code choice}, {@code score} or {@code noul}
     * @param criteria option id → description for {@code choice}; omitted for {@code noul}
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Question(String type, String instructions, Map<String, String> criteria) {

        public static Question choice(String instructions, Map<String, String> criteria) {
            return new Question("choice", instructions, criteria);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Map<String, Answer> answers) {

        public Answer answer(String id) {
            return answers == null ? null : answers.get(id);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Answer(String type, String choice, Double noul, Map<String, Double> probabilities) {

        /** Probability mass on the reported choice. */
        public double choiceProbability() {
            if (choice == null || probabilities == null) {
                return 0.0;
            }
            return probabilities.getOrDefault(choice, 0.0);
        }

        public Map<String, Double> probabilities() {
            return probabilities == null ? Collections.emptyMap() : probabilities;
        }
    }
}
