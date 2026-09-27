package com.example.chess.game;

import com.example.chess.ai.AiService;
import com.example.chess.chess.ChessMove;
import com.example.chess.chess.Color;
import com.example.chess.chess.IllegalMoveException;
import com.example.chess.common.ApiException;
import com.example.chess.common.ErrorCode;
import com.example.chess.engine.ChessEngine;
import com.example.chess.engine.EngineAnalysis;
import com.example.chess.engine.EngineUnavailableException;
import com.example.chess.game.domain.GameStatus;
import com.example.chess.game.repository.MoveRepository;
import com.example.chess.game.service.GameAnalysisService;
import com.example.chess.game.service.GameService;
import com.example.chess.game.service.GameState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class GameServiceTest {

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("test.db"));
    }

    @MockitoBean
    ChessEngine engine;

    @MockitoBean
    AiService aiService;

    @Autowired
    GameService gameService;

    @Autowired
    GameAnalysisService analysisService;

    @Autowired
    MoveRepository moveRepository;

    @Test
    void createsGameWithPlayerToMove() {
        GameState state = gameService.createGame(Color.WHITE);

        assertThat(state.game().getStatus()).isEqualTo(GameStatus.PLAYING);
        assertThat(state.position().turn()).isEqualTo(Color.WHITE);
        assertThat(state.moves()).isEmpty();
        assertThat(state.isPlayersTurn()).isTrue();
        verify(engine, never()).getBestMove(anyString());
    }

    @Test
    void computerOpensWhenPlayerChoosesBlack() {
        when(engine.getBestMove(anyString())).thenReturn("e2e4");

        GameState state = gameService.createGame(Color.BLACK);

        assertThat(state.moves()).extracting(m -> m.getSan()).containsExactly("e4");
        assertThat(state.position().turn()).isEqualTo(Color.BLACK);
    }

    @Test
    void retrievesPersistedGame() {
        UUID id = gameService.createGame(Color.WHITE).game().getId();

        GameState state = gameService.getGame(id);

        assertThat(state.game().getId()).isEqualTo(id);
        assertThat(state.game().getFen()).isEqualTo(com.example.chess.chess.ChessService.STANDARD_START_FEN);
    }

    @Test
    void unknownGameIsNotFound() {
        assertThatThrownBy(() -> gameService.getGame(UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ErrorCode.GAME_NOT_FOUND);
    }

    @Test
    void appliesLegalMoveAndComputerReplyAndPersistsBoth() {
        UUID id = gameService.createGame(Color.WHITE).game().getId();
        when(engine.getBestMove(anyString())).thenReturn("e7e5");

        GameState state = gameService.makeMove(id, new ChessMove("e2", "e4", null));

        assertThat(state.moves()).extracting(m -> m.getSan()).containsExactly("e4", "e5");
        assertThat(state.game().getFen()).isEqualTo("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2");
        assertThat(moveRepository.findByGameIdOrderByPlyAsc(id))
                .extracting(m -> m.getPly() + ":" + m.getMoveNumber() + ":" + m.getColor() + ":" + m.getSan())
                .containsExactly("1:1:WHITE:e4", "2:1:BLACK:e5");
        assertThat(gameService.getGame(id).game().getFen()).isEqualTo(state.game().getFen());
    }

    @Test
    void rejectsIllegalMoveWithoutPersisting() {
        UUID id = gameService.createGame(Color.WHITE).game().getId();

        assertThatThrownBy(() -> gameService.makeMove(id, new ChessMove("e2", "e5", null)))
                .isInstanceOf(IllegalMoveException.class);
        assertThat(moveRepository.findByGameIdOrderByPlyAsc(id)).isEmpty();
        verify(engine, never()).getBestMove(anyString());
    }

    @Test
    void engineFailureLeavesGameUnchanged() {
        UUID id = gameService.createGame(Color.WHITE).game().getId();
        when(engine.getBestMove(anyString())).thenThrow(new EngineUnavailableException("down"));

        assertThatThrownBy(() -> gameService.makeMove(id, new ChessMove("e2", "e4", null)))
                .isInstanceOf(EngineUnavailableException.class);
        assertThat(moveRepository.findByGameIdOrderByPlyAsc(id)).isEmpty();
    }

    @Test
    void illegalEngineMoveIsReportedAsEngineFailure() {
        UUID id = gameService.createGame(Color.WHITE).game().getId();
        when(engine.getBestMove(anyString())).thenReturn("e2e4");

        assertThatThrownBy(() -> gameService.makeMove(id, new ChessMove("d2", "d4", null)))
                .isInstanceOf(EngineUnavailableException.class);
    }

    @Test
    void checkmateFinishesGameAndRejectsFurtherMoves() {
        UUID id = gameService.createGame(Color.WHITE).game().getId();
        when(engine.getBestMove(anyString())).thenReturn("e7e5", "d8h4");

        gameService.makeMove(id, new ChessMove("f2", "f3", null));
        GameState mated = gameService.makeMove(id, new ChessMove("g2", "g4", null));

        assertThat(mated.game().getStatus()).isEqualTo(GameStatus.CHECKMATE);
        assertThat(mated.moves().getLast().getSan()).isEqualTo("Qh4#");
        assertThat(mated.isPlayersTurn()).isFalse();
        assertThatThrownBy(() -> gameService.makeMove(id, new ChessMove("a2", "a3", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ErrorCode.GAME_ALREADY_FINISHED);
    }

    @Test
    void playerCheckmateSkipsComputerMove() {
        UUID id = gameService.createGame(Color.WHITE).game().getId();
        when(engine.getBestMove(anyString())).thenReturn("f7f6", "g7g5");

        gameService.makeMove(id, new ChessMove("e2", "e4", null));
        gameService.makeMove(id, new ChessMove("d2", "d4", null));
        GameState state = gameService.makeMove(id, new ChessMove("d1", "h5", null));

        assertThat(state.game().getStatus()).isEqualTo(GameStatus.CHECKMATE);
        assertThat(state.moves()).hasSize(5);
    }

    @Test
    void analysisPassesEngineOutputToAi() {
        UUID id = gameService.createGame(Color.WHITE).game().getId();
        EngineAnalysis engineAnalysis = new EngineAnalysis("e2e4", 0.3, 18, null, List.of("e2e4"));
        when(engine.analyze(anyString())).thenReturn(engineAnalysis);
        when(aiService.explainPosition(anyString(), eq(null), eq(engineAnalysis))).thenReturn("explained");

        GameAnalysisService.PositionAnalysis analysis = analysisService.analyze(id);

        assertThat(analysis.bestMoveSan()).isEqualTo("e4");
        assertThat(analysis.text()).isEqualTo("explained");
        assertThat(analysis.engine().depth()).isEqualTo(18);
    }
}
