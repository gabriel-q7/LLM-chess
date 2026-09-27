package com.example.chess.ai;

import com.example.chess.engine.EngineAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Assistant backed by Laya. Laya reads the player's question and the engine facts and returns
 * typed decisions (what the player asks about, which theme matters); the reply text is composed
 * from engine facts, so it cannot contain moves the engine did not produce.
 */
@Service
public class LayaAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(LayaAiService.class);

    private final LayaClient client;
    private final PromptBuilder promptBuilder;
    private final ExplanationComposer composer;
    private final LayaProperties properties;

    public LayaAiService(LayaClient client, PromptBuilder promptBuilder, ExplanationComposer composer,
                         LayaProperties properties) {
        this.client = client;
        this.promptBuilder = promptBuilder;
        this.composer = composer;
        this.properties = properties;
    }

    @Override
    public String explainPosition(String fen, String lastMove, EngineAnalysis analysis) {
        PositionFacts facts = promptBuilder.facts(fen, lastMove, analysis);
        LayaModels.Response response = client.decide(promptBuilder.positionRequest(facts, properties.model()));
        return composer.explain(facts, confidentChoice(response, PromptBuilder.FOCUS));
    }

    @Override
    public String answerQuestion(String question, String fen, String lastMove, EngineAnalysis analysis) {
        PositionFacts facts = promptBuilder.facts(fen, lastMove, analysis);
        LayaModels.Response intent = client.decide(promptBuilder.intentRequest(question, properties.model()));
        LayaModels.Response position = client.decide(promptBuilder.positionRequest(facts, properties.model()));
        return composer.answer(confidentChoice(intent, PromptBuilder.INTENT), facts,
                confidentChoice(position, PromptBuilder.FOCUS));
    }

    /** Laya's choice for {@code questionId}, or {@code null} when it is below the confidence threshold. */
    private String confidentChoice(LayaModels.Response response, String questionId) {
        LayaModels.Answer answer = response.answer(questionId);
        if (answer == null || answer.choice() == null) {
            return null;
        }
        double probability = answer.choiceProbability();
        log.debug("Laya {} = {} (p={})", questionId, answer.choice(), probability);
        return probability >= properties.minConfidence() ? answer.choice() : null;
    }
}
