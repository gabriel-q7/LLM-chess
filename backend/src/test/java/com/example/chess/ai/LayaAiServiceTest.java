package com.example.chess.ai;

import com.example.chess.chess.ChessService;
import com.example.chess.engine.EngineAnalysis;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LayaAiServiceTest {

    // After 1.e4 e5 2.Nf3, Black to move.
    private static final String FEN = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2";
    private static final EngineAnalysis ANALYSIS =
            new EngineAnalysis("b8c6", 0.35, 18, null, List.of("b8c6", "f1b5", "a7a6"));

    private final LayaClient client = mock(LayaClient.class);
    private final LayaProperties properties = new LayaProperties("http://laya:8000", "english", null, Duration.ofSeconds(5), 0.5);
    private final LayaAiService service = new LayaAiService(client, new PromptBuilder(new ChessService()),
            new ExplanationComposer(), properties);

    /** Both Laya requests answer from the same map; each reads only its own question id. */
    private static LayaModels.Response answers(String intent, double intentP, String focus, double focusP) {
        return new LayaModels.Response(Map.of(
                PromptBuilder.INTENT, new LayaModels.Answer("choice", intent, null, Map.of(intent, intentP)),
                PromptBuilder.FOCUS, new LayaModels.Answer("choice", focus, null, Map.of(focus, focusP))));
    }

    @Test
    void explanationStatesEngineFactsAndConfidentTheme() {
        when(client.decide(any())).thenReturn(answers("other", 0.1, "development", 0.8));

        String text = service.explainPosition(FEN, "Nf3", ANALYSIS);

        assertThat(text)
                .contains("After Nf3")
                .contains("White is slightly better (+0.35 at depth 18)")
                .contains("Stockfish's best move for Black is Nc6")
                .contains("Nc6 Bb5 a6")
                .contains("Material: equal")
                .contains("The key theme is development");
    }

    @Test
    void lowConfidenceThemeIsLeftOut() {
        when(client.decide(any())).thenReturn(answers("other", 0.1, "endgame", 0.3));

        String text = service.explainPosition(FEN, "Nf3", ANALYSIS);

        assertThat(text).doesNotContain("key theme");
    }

    @Test
    void sendsStructuredEngineContextToLaya() {
        when(client.decide(any())).thenReturn(answers("best_move", 0.9, "development", 0.2));
        ArgumentCaptor<LayaModels.Request> request = ArgumentCaptor.forClass(LayaModels.Request.class);

        service.answerQuestion("What should I play?", FEN, "Nf3", ANALYSIS);

        verify(client, times(2)).decide(request.capture());
        LayaModels.Request intent = request.getAllValues().get(0);
        LayaModels.Request position = request.getAllValues().get(1);
        assertThat(intent.model()).isEqualTo("english");
        assertThat(intent.state()).isEqualTo("A chess player asks: What should I play?");
        assertThat(intent.questions()).containsOnlyKeys(PromptBuilder.INTENT);
        assertThat(position.questions()).containsOnlyKeys(PromptBuilder.FOCUS);
        assertThat(position.state())
                .contains("FEN: " + FEN)
                .contains("Last move: Nf3")
                .contains("Best move: Nc6")
                .contains("Evaluation: +0.35")
                .contains("Search depth: 18");
    }

    @Test
    void answersByIntent() {
        when(client.decide(any())).thenReturn(answers("best_move", 0.9, "development", 0.2));
        assertThat(service.answerQuestion("hint?", FEN, "Nf3", ANALYSIS)).startsWith("Stockfish's best move for Black is Nc6.");

        when(client.decide(any())).thenReturn(answers("evaluation", 0.9, "development", 0.2));
        assertThat(service.answerQuestion("who is winning?", FEN, "Nf3", ANALYSIS))
                .isEqualTo("White is slightly better (+0.35). Material: equal.");

        when(client.decide(any())).thenReturn(answers("last_move", 0.9, "development", 0.2));
        assertThat(service.answerQuestion("why that move?", FEN, "Nf3", ANALYSIS))
                .isEqualTo("Nf3 was the last move. After it, White is slightly better (+0.35). "
                        + "The expected continuation is Nc6 Bb5 a6.");

        when(client.decide(any())).thenReturn(answers("other", 0.9, "development", 0.2));
        assertThat(service.answerQuestion("what's the weather?", FEN, "Nf3", ANALYSIS)).startsWith("I can only talk about this game");
    }

    @Test
    void uncertainIntentFallsBackToSummary() {
        when(client.decide(any())).thenReturn(answers("plan", 0.3, "development", 0.2));

        assertThat(service.answerQuestion("hmm", FEN, "Nf3", ANALYSIS)).startsWith("I'm not sure what you are asking.");
    }

    @Test
    void reportsMateAndGameOver() {
        when(client.decide(any())).thenReturn(answers("evaluation", 0.9, "attack", 0.2));
        String mated = "rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3";

        assertThat(service.answerQuestion("who wins?", mated, "Qh4#", new EngineAnalysis(null, -100, 0, 0, List.of())))
                .isEqualTo("The game is over: White is checkmated.");
    }
}
