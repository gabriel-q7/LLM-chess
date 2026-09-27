package com.example.chess.ai;

import com.example.chess.chess.ChessService;
import com.example.chess.chess.Color;
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

    // After 1.e4 e5 2.Nf3, Black (the player) to move.
    private static final String FEN = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2";
    private static final EngineAnalysis ANALYSIS =
            new EngineAnalysis("b8c6", -0.35, 18, null, List.of("b8c6", "f1b5", "a7a6"));

    // Scholar's mate threat: White (the player) mates with Qxf7#.
    private static final String MATE_FEN = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4";
    private static final EngineAnalysis MATE = new EngineAnalysis("h5f7", 100, 20, 1, List.of("h5f7"));

    private final LayaClient client = mock(LayaClient.class);
    private final LayaProperties properties = new LayaProperties("http://laya:8000", "english", null, Duration.ofSeconds(5), 0.5);
    private final LayaAiService service = new LayaAiService(client, new PromptBuilder(new ChessService()),
            new ExplanationComposer(), properties);

    private static PositionContext context(Disclosure disclosure) {
        return new PositionContext(FEN, "Nf3", ANALYSIS, Color.BLACK, disclosure);
    }

    /** Both Laya requests answer from the same map; each reads only its own question id. */
    private static LayaModels.Response answers(String intent, double intentP, String focus, double focusP) {
        return new LayaModels.Response(Map.of(
                PromptBuilder.INTENT, new LayaModels.Answer("choice", intent, null, Map.of(intent, intentP)),
                PromptBuilder.FOCUS, new LayaModels.Answer("choice", focus, null, Map.of(focus, focusP))));
    }

    private List<String> statesSent() {
        ArgumentCaptor<LayaModels.Request> request = ArgumentCaptor.forClass(LayaModels.Request.class);
        verify(client, org.mockito.Mockito.atLeastOnce()).decide(request.capture());
        return request.getAllValues().stream().map(LayaModels.Request::state).toList();
    }

    @Test
    void easyExplainsBestMoveScoreAndLine() {
        when(client.decide(any())).thenReturn(answers("other", 0.1, "development", 0.8));

        String text = service.explainPosition(context(Disclosure.FULL));

        assertThat(text)
                .contains("After Nf3, Black is slightly better (-0.35 at depth 18)")
                .contains("Stockfish's best move for you is Nc6")
                .contains("Nc6 Bb5 a6")
                .contains("Material: equal")
                .contains("The key theme is development");
        assertThat(statesSent().getFirst()).contains("Best move: Nc6", "Evaluation: -0.35", "Expected continuation");
    }

    @Test
    void mediumGivesPieceHintButNeverTheMoveOrScore() {
        when(client.decide(any())).thenReturn(answers("best_move", 0.9, "development", 0.2));

        String explanation = service.explainPosition(context(Disclosure.HINTS));
        String answer = service.answerQuestion("What should I play?", context(Disclosure.HINTS));

        assertThat(explanation).contains("Black is slightly better").contains("Hint: consider a move with your knight.");
        assertThat(answer).isEqualTo("Hint: consider a move with your knight.");
        for (String text : List.of(explanation, answer)) {
            assertThat(text).doesNotContain("Nc6", "Bb5", "0.35", "depth");
        }
        assertThat(statesSent()).allSatisfy(state ->
                assertThat(state).doesNotContain("Best move", "Nc6", "Bb5", "0.35", "FEN", FEN));
    }

    @Test
    void hardRevealsNoMoveHintOrScore() {
        when(client.decide(any())).thenReturn(answers("best_move", 0.9, "development", 0.2));

        String explanation = service.explainPosition(context(Disclosure.MINIMAL));
        String answer = service.answerQuestion("What should I play?", context(Disclosure.MINIMAL));

        assertThat(explanation).contains("Black is slightly better").doesNotContain("Hint", "knight");
        assertThat(answer).isEqualTo(ExplanationComposer.NO_HINTS);
        for (String text : List.of(explanation, answer)) {
            assertThat(text).doesNotContain("Nc6", "Bb5", "0.35", "depth");
        }
        assertThat(statesSent()).allSatisfy(state ->
                assertThat(state).doesNotContain("Best move", "Piece to consider", "Nc6", "0.35", "FEN"));
    }

    @Test
    void playerMateIsDisclosedAccordingToLevel() {
        when(client.decide(any())).thenReturn(answers("threats", 0.9, "attack", 0.2));

        assertThat(service.answerQuestion("threats?", new PositionContext(MATE_FEN, "Nf6", MATE, Color.WHITE, Disclosure.FULL)))
                .contains("You have a forced mate in 1.").contains("Qxf7#");
        assertThat(service.answerQuestion("threats?", new PositionContext(MATE_FEN, "Nf6", MATE, Color.WHITE, Disclosure.HINTS)))
                .contains("You have a forced mate: look for it!").doesNotContain("Qxf7", "in 1");
        assertThat(service.answerQuestion("threats?", new PositionContext(MATE_FEN, "Nf6", MATE, Color.WHITE, Disclosure.MINIMAL)))
                .isEqualTo("There is no check or forced mate against you right now.");
    }

    @Test
    void mateAgainstThePlayerIsAlwaysReported() {
        when(client.decide(any())).thenReturn(answers("threats", 0.9, "attack", 0.2));
        // Same position, but the human plays Black and is about to be mated.
        assertThat(service.answerQuestion("am I in danger?", new PositionContext(MATE_FEN, "Nf6", MATE, Color.BLACK, Disclosure.MINIMAL)))
                .contains("Your opponent threatens a forced mate in 1.")
                .doesNotContain("Qxf7");
    }

    @Test
    void balanceIsNotCapitalisedMidSentence() {
        when(client.decide(any())).thenReturn(answers("other", 0.1, "development", 0.1));
        EngineAnalysis equal = new EngineAnalysis("b8c6", 0.1, 18, null, List.of("b8c6"));

        assertThat(service.explainPosition(new PositionContext(FEN, "Nf3", equal, Color.BLACK, Disclosure.FULL)))
                .startsWith("After Nf3, the position is roughly equal (+0.10");
        assertThat(service.explainPosition(new PositionContext(FEN, null, equal, Color.BLACK, Disclosure.FULL)))
                .startsWith("The position is roughly equal (+0.10");
    }

    @Test
    void lowConfidenceThemeIsLeftOut() {
        when(client.decide(any())).thenReturn(answers("other", 0.1, "endgame", 0.3));

        assertThat(service.explainPosition(context(Disclosure.FULL))).doesNotContain("key theme");
    }

    @Test
    void classifiesTheQuestionSeparatelyFromTheFacts() {
        when(client.decide(any())).thenReturn(answers("best_move", 0.9, "development", 0.2));
        ArgumentCaptor<LayaModels.Request> request = ArgumentCaptor.forClass(LayaModels.Request.class);

        service.answerQuestion("What should I play?", context(Disclosure.FULL));

        verify(client, times(2)).decide(request.capture());
        LayaModels.Request intent = request.getAllValues().get(0);
        LayaModels.Request position = request.getAllValues().get(1);
        assertThat(intent.model()).isEqualTo("english");
        assertThat(intent.state()).isEqualTo("A chess player asks: What should I play?");
        assertThat(intent.questions()).containsOnlyKeys(PromptBuilder.INTENT);
        assertThat(position.questions()).containsOnlyKeys(PromptBuilder.FOCUS);
        assertThat(position.state()).contains("Last move: Nf3", "Assessment: Black is slightly better");
    }

    @Test
    void answersByIntent() {
        when(client.decide(any())).thenReturn(answers("evaluation", 0.9, "development", 0.2));
        assertThat(service.answerQuestion("who is winning?", context(Disclosure.FULL)))
                .isEqualTo("Black is slightly better (-0.35 at depth 18). Material: equal.");

        when(client.decide(any())).thenReturn(answers("last_move", 0.9, "development", 0.2));
        assertThat(service.answerQuestion("why that move?", context(Disclosure.FULL)))
                .isEqualTo("Nf3 was the last move. After it, Black is slightly better (-0.35 at depth 18). "
                        + "The expected continuation is Nc6 Bb5 a6.");

        when(client.decide(any())).thenReturn(answers("other", 0.9, "development", 0.2));
        assertThat(service.answerQuestion("what's the weather?", context(Disclosure.FULL))).startsWith("I can only talk about this game");
    }

    @Test
    void uncertainIntentFallsBackToSummary() {
        when(client.decide(any())).thenReturn(answers("plan", 0.3, "development", 0.2));

        assertThat(service.answerQuestion("hmm", context(Disclosure.MINIMAL)))
                .isEqualTo("I'm not sure what you are asking. Black is slightly better.");
    }

    @Test
    void reportsGameOver() {
        when(client.decide(any())).thenReturn(answers("evaluation", 0.9, "attack", 0.2));
        String mated = "rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3";

        assertThat(service.answerQuestion("who wins?",
                new PositionContext(mated, "Qh4#", new EngineAnalysis(null, -100, 0, 0, List.of()), Color.WHITE, Disclosure.MINIMAL)))
                .isEqualTo("The game is over: White is checkmated.");
    }
}
