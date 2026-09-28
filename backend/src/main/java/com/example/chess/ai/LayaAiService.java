package com.example.chess.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assistant backed by Laya. Laya reads the player's question and the disclosed facts and returns
 * typed decisions (what the player asks about, which theme matters); the reply text is composed
 * from those facts, so it cannot contain moves the engine did not produce or the level hides.
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
    public AiReply explainPosition(PositionContext context) {
        PositionFacts facts = promptBuilder.facts(context);
        Trace trace = new Trace(context.disclosure());
        LayaModels.Response response = trace.decide(promptBuilder.positionRequest(facts, properties.model()));
        return new AiReply(composer.explain(facts, trace.confidentChoice(response, PromptBuilder.FOCUS)), trace.build());
    }

    @Override
    public AiReply answerQuestion(String question, PositionContext context) {
        PositionFacts facts = promptBuilder.facts(context);
        Trace trace = new Trace(context.disclosure());
        LayaModels.Response intent = trace.decide(promptBuilder.intentRequest(question, properties.model()));
        LayaModels.Response position = trace.decide(promptBuilder.positionRequest(facts, properties.model()));
        String text = composer.answer(trace.confidentChoice(intent, PromptBuilder.INTENT), facts,
                trace.confidentChoice(position, PromptBuilder.FOCUS));
        return new AiReply(text, trace.build());
    }

    /** Sends requests to Laya and records each exchange for the reply's {@link AiTrace}. */
    private final class Trace {

        private final Disclosure disclosure;
        private final List<AiTrace.Exchange> exchanges = new ArrayList<>();

        Trace(Disclosure disclosure) {
            this.disclosure = disclosure;
        }

        LayaModels.Response decide(LayaModels.Request request) {
            LayaModels.Response response = client.decide(request);
            Map<String, AiTrace.Decision> decisions = new LinkedHashMap<>();
            for (String id : request.questions().keySet()) {
                LayaModels.Answer answer = response.answer(id);
                if (answer != null) {
                    double p = answer.choiceProbability();
                    decisions.put(id, new AiTrace.Decision(answer.choice(), p, p >= properties.minConfidence(),
                            answer.probabilities()));
                }
            }
            exchanges.add(new AiTrace.Exchange(request.model(), request.state(), request.questions(), decisions));
            return response;
        }

        /** Laya's choice for {@code questionId}, or {@code null} when it is below the confidence threshold. */
        String confidentChoice(LayaModels.Response response, String questionId) {
            LayaModels.Answer answer = response.answer(questionId);
            if (answer == null || answer.choice() == null) {
                return null;
            }
            double probability = answer.choiceProbability();
            log.debug("Laya {} = {} (p={})", questionId, answer.choice(), probability);
            return probability >= properties.minConfidence() ? answer.choice() : null;
        }

        AiTrace build() {
            return new AiTrace(disclosure, AiTrace.hiddenFacts(disclosure), List.copyOf(exchanges));
        }
    }
}
