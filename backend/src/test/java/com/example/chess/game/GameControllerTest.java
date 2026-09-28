package com.example.chess.game;

import com.example.chess.ai.AiServiceUnavailableException;
import com.example.chess.chess.ChessMove;
import com.example.chess.chess.ChessService;
import com.example.chess.chess.Color;
import com.example.chess.chess.IllegalMoveException;
import com.example.chess.common.ApiException;
import com.example.chess.common.ErrorCode;
import com.example.chess.engine.EngineUnavailableException;
import com.example.chess.game.controller.GameController;
import com.example.chess.game.domain.Difficulty;
import com.example.chess.game.domain.Game;
import com.example.chess.game.service.GameAnalysisService;
import com.example.chess.game.service.GameService;
import com.example.chess.game.service.GameState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameController.class)
class GameControllerTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GameService gameService;

    @MockitoBean
    GameAnalysisService analysisService;

    private static GameState newGame() {
        return newGame(Difficulty.MEDIUM);
    }

    private static GameState newGame(Difficulty difficulty) {
        ChessService chess = new ChessService();
        Game game = new Game(ID, ChessService.STANDARD_START_FEN, Color.WHITE, difficulty, LocalDateTime.now());
        return new GameState(game, List.of(), chess.position(ChessService.STANDARD_START_FEN, List.of()));
    }

    @Test
    void createsGame() throws Exception {
        when(gameService.createGame(Color.WHITE, Difficulty.MEDIUM)).thenReturn(newGame());

        mvc.perform(post("/api/games"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.difficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.status").value("PLAYING"))
                .andExpect(jsonPath("$.turn").value("WHITE"))
                .andExpect(jsonPath("$.fen").value(ChessService.STANDARD_START_FEN))
                .andExpect(jsonPath("$.moves", hasSize(0)))
                .andExpect(jsonPath("$.legalMoves", hasSize(20)));
    }

    @Test
    void createsGameWithChosenDifficulty() throws Exception {
        when(gameService.createGame(Color.BLACK, Difficulty.HARD)).thenReturn(newGame(Difficulty.HARD));

        mvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerColor\":\"BLACK\",\"difficulty\":\"HARD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.difficulty").value("HARD"));
    }

    @Test
    void hiddenAnalysisFieldsAreNull() throws Exception {
        when(analysisService.analyze(ID)).thenReturn(
                new GameAnalysisService.PositionAnalysis(null, null, null, 18, "Black is slightly better.", null));

        mvc.perform(post("/api/games/{id}/analysis", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestMove").doesNotExist())
                .andExpect(jsonPath("$.evaluation").doesNotExist())
                .andExpect(jsonPath("$.explanation").value("Black is slightly better."));
    }

    @Test
    void engineContextIsEmptyBeforeTheComputerMoves() throws Exception {
        when(gameService.lastComputerMoveContext(ID)).thenReturn(java.util.Optional.empty());

        mvc.perform(get("/api/games/{id}/engine-context", ID)).andExpect(status().isNoContent());
    }

    @Test
    void submitsMove() throws Exception {
        when(gameService.makeMove(ID, new ChessMove("e2", "e4", null))).thenReturn(newGame());

        mvc.perform(post("/api/games/{id}/moves", ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"e2\",\"to\":\"e4\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void validatesMoveRequest() throws Exception {
        mvc.perform(post("/api/games/{id}/moves", ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"z9\",\"to\":\"e4\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void mapsIllegalMove() throws Exception {
        when(gameService.makeMove(eq(ID), any())).thenThrow(new IllegalMoveException("Illegal move e2-e5"));

        mvc.perform(post("/api/games/{id}/moves", ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"e2\",\"to\":\"e5\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ILLEGAL_MOVE"));
    }

    @Test
    void mapsNotFoundAndFinished() throws Exception {
        when(gameService.getGame(ID)).thenThrow(new ApiException(ErrorCode.GAME_NOT_FOUND));
        mvc.perform(get("/api/games/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"));

        when(gameService.makeMove(eq(ID), any())).thenThrow(new ApiException(ErrorCode.GAME_ALREADY_FINISHED));
        mvc.perform(post("/api/games/{id}/moves", ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"e2\",\"to\":\"e4\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_ALREADY_FINISHED"));
    }

    @Test
    void hidesInfrastructureDetails() throws Exception {
        when(gameService.makeMove(eq(ID), any()))
                .thenThrow(new EngineUnavailableException("Connection refused: stockfish:4000"));
        mvc.perform(post("/api/games/{id}/moves", ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"e2\",\"to\":\"e4\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ENGINE_UNAVAILABLE"))
                .andExpect(content().string(not(containsString("stockfish:4000"))));

        when(analysisService.analyze(ID)).thenThrow(new AiServiceUnavailableException("http://laya:8000 refused", null));
        mvc.perform(post("/api/games/{id}/analysis", ID))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_SERVICE_UNAVAILABLE"))
                .andExpect(content().string(not(containsString("laya:8000"))));

        when(gameService.getGame(ID)).thenThrow(new IllegalStateException("boom at /internal/path"));
        mvc.perform(get("/api/games/{id}", ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("/internal/path"))));
    }

    @Test
    void chatRequiresQuestion() throws Exception {
        mvc.perform(post("/api/games/{id}/chat", ID).contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
