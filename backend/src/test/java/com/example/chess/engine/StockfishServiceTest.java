package com.example.chess.engine;

import com.example.chess.chess.ChessService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockfishServiceTest {

    private static final String WHITE_TO_MOVE = ChessService.STANDARD_START_FEN;
    private static final String BLACK_TO_MOVE = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1";

    private final EngineProperties properties = new EngineProperties("localhost", 4000, null, 10, 18, 5, Duration.ofSeconds(5));
    private final StockfishClient client = mock(StockfishClient.class);
    private final StockfishService service = new StockfishService(client, new ChessService(), properties);

    @Test
    void parsesPrimaryLineAndIgnoresBoundsAndOtherLines() {
        StockfishClient.UciResult.Builder builder = new StockfishClient.UciResult.Builder();
        builder.acceptInfo("info string NNUE evaluation using nn-1111cefa1111.nnue enabled");
        builder.acceptInfo("info depth 17 seldepth 22 multipv 1 score cp 31 nodes 1 nps 1 pv e2e4 e7e5");
        builder.acceptInfo("info depth 18 seldepth 24 multipv 1 score cp 90 lowerbound nodes 2 pv d2d4");
        builder.acceptInfo("info depth 18 seldepth 24 multipv 1 score cp 35 nodes 3 nps 1 pv e2e4 c7c5 g1f3");
        builder.acceptInfo("info depth 18 currmove g1f3 currmovenumber 2");
        builder.acceptBestMove("bestmove e2e4 ponder c7c5");

        StockfishClient.UciResult result = builder.build();

        assertThat(result.bestMove()).isEqualTo("e2e4");
        assertThat(result.depth()).isEqualTo(18);
        assertThat(result.scoreCp()).isEqualTo(35);
        assertThat(result.mateIn()).isNull();
        assertThat(result.pv()).containsExactly("e2e4", "c7c5", "g1f3");
    }

    @Test
    void parsesMateScoresAndNoMove() {
        StockfishClient.UciResult.Builder builder = new StockfishClient.UciResult.Builder();
        builder.acceptInfo("info depth 0 score mate 0");
        builder.acceptBestMove("bestmove (none)");

        StockfishClient.UciResult result = builder.build();

        assertThat(result.bestMove()).isNull();
        assertThat(result.mateIn()).isZero();
    }

    @Test
    void normalisesScoreToWhitePerspective() {
        when(client.search(eq(BLACK_TO_MOVE), eq(18), eq(20)))
                .thenReturn(new StockfishClient.UciResult("c7c5", 18, 40, null, List.of("c7c5", "g1f3")));

        EngineAnalysis analysis = service.analyze(BLACK_TO_MOVE);

        assertThat(analysis.evaluation()).isEqualTo(-0.40);
        assertThat(analysis.bestMove()).isEqualTo("c7c5");
        assertThat(analysis.principalVariation()).containsExactly("c7c5", "g1f3");
    }

    @Test
    void normalisesMateToWhitePerspective() {
        when(client.search(eq(BLACK_TO_MOVE), anyInt(), anyInt()))
                .thenReturn(new StockfishClient.UciResult("d8h4", 5, null, 1, List.of("d8h4")));

        EngineAnalysis analysis = service.analyze(BLACK_TO_MOVE);

        assertThat(analysis.mateIn()).isEqualTo(-1);
        assertThat(analysis.evaluation()).isEqualTo(-EngineAnalysis.MATE_SCORE);
    }

    @Test
    void computerMoveUsesConfiguredDepthAndSkill() {
        when(client.search(WHITE_TO_MOVE, 10, 5)).thenReturn(new StockfishClient.UciResult("e2e4", 10, 30, null, List.of()));

        assertThat(service.getBestMove(WHITE_TO_MOVE)).isEqualTo("e2e4");
    }

    @Test
    void failsWhenEngineReturnsNoMove() {
        when(client.search(anyString(), anyInt(), anyInt())).thenReturn(new StockfishClient.UciResult(null, 0, 0, null, List.of()));

        assertThatThrownBy(() -> service.getBestMove(WHITE_TO_MOVE)).isInstanceOf(EngineUnavailableException.class);
    }
}
