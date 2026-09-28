package com.example.chess.game.controller;

import com.example.chess.chess.ChessMove;
import com.example.chess.game.controller.dto.AnalysisResponse;
import com.example.chess.game.controller.dto.ChatRequest;
import com.example.chess.game.controller.dto.ChatResponse;
import com.example.chess.game.controller.dto.CreateGameRequest;
import com.example.chess.game.controller.dto.GameResponse;
import com.example.chess.game.controller.dto.MoveRequest;
import com.example.chess.game.service.GameAnalysisService;
import com.example.chess.game.service.ComputerMoveContext;
import com.example.chess.game.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final GameAnalysisService analysisService;

    public GameController(GameService gameService, GameAnalysisService analysisService) {
        this.gameService = gameService;
        this.analysisService = analysisService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameResponse create(@RequestBody(required = false) CreateGameRequest request) {
        CreateGameRequest body = request == null ? new CreateGameRequest(null, null) : request;
        return GameResponse.from(gameService.createGame(body.playerColorOrDefault(), body.difficultyOrDefault()));
    }

    @GetMapping("/{id}")
    public GameResponse get(@PathVariable UUID id) {
        return GameResponse.from(gameService.getGame(id));
    }

    @PostMapping("/{id}/moves")
    public GameResponse move(@PathVariable UUID id, @Valid @RequestBody MoveRequest request) {
        ChessMove move = new ChessMove(request.from(), request.to(), request.promotion());
        return GameResponse.from(gameService.makeMove(id, move));
    }

    /** Debug view: what Stockfish received and returned for the latest computer move (204 when there is none). */
    @GetMapping("/{id}/engine-context")
    public ResponseEntity<ComputerMoveContext> engineContext(@PathVariable UUID id) {
        return gameService.lastComputerMoveContext(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/analysis")
    public AnalysisResponse analysis(@PathVariable UUID id) {
        return AnalysisResponse.from(analysisService.analyze(id));
    }

    @PostMapping("/{id}/chat")
    public ChatResponse chat(@PathVariable UUID id, @Valid @RequestBody ChatRequest request) {
        return ChatResponse.from(analysisService.answer(id, request.question()));
    }
}
