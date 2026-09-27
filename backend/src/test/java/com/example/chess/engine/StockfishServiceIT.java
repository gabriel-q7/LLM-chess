package com.example.chess.engine;

import com.example.chess.chess.ChessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against a real Stockfish. Start one with
 * {@code docker compose -f infrastructure/docker-compose.yml up -d stockfish} and run
 * {@code STOCKFISH_HOST=localhost ./mvnw verify}.
 */
@EnabledIfEnvironmentVariable(named = "STOCKFISH_HOST", matches = ".+")
class StockfishServiceIT {

    private final EngineProperties properties = new EngineProperties(
            System.getenv("STOCKFISH_HOST"),
            Integer.parseInt(System.getenv().getOrDefault("STOCKFISH_PORT", "4000")),
            null, 8, 12, 20, Duration.ofSeconds(30));
    private final ChessService chess = new ChessService();
    private final StockfishService engine = new StockfishService(new StockfishClient(properties), chess, properties);

    @Test
    void returnsALegalMoveFromTheStartingPosition() {
        String move = engine.getBestMove(ChessService.STANDARD_START_FEN);

        assertThat(chess.position(ChessService.STANDARD_START_FEN, java.util.List.of()).legalMoves())
                .extracting(m -> m.toUci())
                .contains(move);
    }

    @Test
    void findsMateInOne() {
        // White to play Qh5xf7#.
        String fen = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4";

        EngineAnalysis analysis = engine.analyze(fen);

        assertThat(analysis.bestMove()).isEqualTo("h5f7");
        assertThat(analysis.mateIn()).isEqualTo(1);
        assertThat(analysis.evaluation()).isEqualTo(EngineAnalysis.MATE_SCORE);
        assertThat(analysis.depth()).isPositive();
    }

    @Test
    void reportsNoMoveWhenCheckmated() {
        String fen = "rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3";

        EngineAnalysis analysis = engine.analyze(fen);

        assertThat(analysis.bestMove()).isNull();
        assertThat(analysis.mateIn()).isZero();
        assertThat(analysis.evaluation()).isEqualTo(-EngineAnalysis.MATE_SCORE);
    }
}
