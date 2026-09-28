package com.example.chess.game.service;

import com.example.chess.chess.AppliedMove;
import com.example.chess.chess.ChessMove;
import com.example.chess.chess.ChessPosition;
import com.example.chess.chess.ChessService;
import com.example.chess.chess.Color;
import com.example.chess.chess.IllegalMoveException;
import com.example.chess.chess.PositionOutcome;
import com.example.chess.common.ApiException;
import com.example.chess.common.ErrorCode;
import com.example.chess.engine.ChessEngine;
import com.example.chess.engine.EngineMove;
import com.example.chess.engine.EngineUnavailableException;
import com.example.chess.game.domain.Difficulty;
import com.example.chess.game.domain.Game;
import com.example.chess.game.domain.GameStatus;
import com.example.chess.game.domain.Move;
import com.example.chess.game.repository.GameRepository;
import com.example.chess.game.repository.MoveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Human-versus-computer game flow. Moves are validated by {@link ChessService}; the computer's
 * reply comes from the {@link ChessEngine}. The engine is consulted before anything is written,
 * so the player's move and the computer's reply are persisted together or not at all.
 */
@Service
public class GameService {

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final ChessService chessService;
    private final ChessEngine engine;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final JsonMapper json;

    public GameService(GameRepository gameRepository, MoveRepository moveRepository, ChessService chessService,
                       ChessEngine engine, TransactionTemplate transaction, Clock clock, JsonMapper json) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.chessService = chessService;
        this.engine = engine;
        this.transaction = transaction;
        this.clock = clock;
        this.json = json;
    }

    public GameState createGame(Color playerColor, Difficulty difficulty) {
        LocalDateTime now = LocalDateTime.now(clock);
        Game game = new Game(UUID.randomUUID(), ChessService.STANDARD_START_FEN, playerColor, difficulty, now);
        ChessPosition start = chessService.position(game.getInitialFen(), List.of());
        List<NewMove> applied = new ArrayList<>();
        if (start.turn() == game.getComputerColor()) {
            applied.add(computerMove(game, List.of(), start, 1));
        }
        return persist(game, List.of(), applied, start);
    }

    public GameState getGame(UUID id) {
        Game game = findGame(id);
        List<Move> moves = moveRepository.findByGameIdOrderByPlyAsc(id);
        return new GameState(game, moves, chessService.position(game.getInitialFen(), history(moves)));
    }

    public GameState makeMove(UUID id, ChessMove move) {
        GameState state = getGame(id);
        Game game = state.game();
        if (game.getStatus().isFinished()) {
            throw new ApiException(ErrorCode.GAME_ALREADY_FINISHED);
        }
        if (state.position().turn() != game.getPlayerColor()) {
            throw new ApiException(ErrorCode.NOT_YOUR_TURN);
        }

        List<ChessMove> history = history(state.moves());
        List<NewMove> applied = new ArrayList<>();
        AppliedMove playerMove = chessService.applyMove(game.getInitialFen(), history, move);
        applied.add(new NewMove(playerMove, null));

        if (!playerMove.position().outcome().isFinished()) {
            List<ChessMove> afterPlayer = new ArrayList<>(history);
            afterPlayer.add(playerMove.move());
            applied.add(computerMove(game, afterPlayer, playerMove.position(), history.size() + 2));
        }
        return persist(game, state.moves(), applied, state.position());
    }

    /** The engine's input and output for the game's latest computer move, if any. */
    public Optional<ComputerMoveContext> lastComputerMoveContext(UUID id) {
        findGame(id);
        return moveRepository.findFirstByGameIdAndEngineContextIsNotNullOrderByPlyDesc(id)
                .map(move -> json.readValue(move.getEngineContext(), ComputerMoveContext.class));
    }

    private NewMove computerMove(Game game, List<ChessMove> history, ChessPosition position, int ply) {
        EngineMove engineMove = engine.getBestMove(position.fen(), game.getDifficulty().strength());
        AppliedMove applied;
        try {
            applied = chessService.applyMove(game.getInitialFen(), history, ChessMove.fromUci(engineMove.move()));
        } catch (IllegalArgumentException | IllegalMoveException e) {
            throw new EngineUnavailableException("Engine proposed an illegal move: " + engineMove.move(), e);
        }
        ComputerMoveContext context = new ComputerMoveContext(ply, applied.san(), position.fen(), game.getDifficulty(),
                engineMove, chessService.toSanLine(position.fen(), engineMove.principalVariation()));
        return new NewMove(applied, json.writeValueAsString(context));
    }

    /** A move about to be stored; {@code engineContext} is set for computer moves. */
    private record NewMove(AppliedMove applied, String engineContext) {
    }

    private GameState persist(Game game, List<Move> existing, List<NewMove> applied, ChessPosition current) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Move> newMoves = new ArrayList<>();
        int ply = existing.size();
        for (NewMove newMove : applied) {
            ply++;
            AppliedMove move = newMove.applied();
            newMoves.add(new Move(UUID.randomUUID(), game.getId(), ply, move.moveNumber(), move.color(),
                    move.move(), move.san(), move.position().fen(), newMove.engineContext(), now));
        }
        ChessPosition finalPosition = applied.isEmpty() ? current : applied.getLast().applied().position();
        game.updatePosition(finalPosition.fen(), toStatus(finalPosition.outcome()), now);

        Game saved = transaction.execute(tx -> {
            // Flushing here surfaces an optimistic-lock conflict as a translated Spring exception.
            Game result = gameRepository.saveAndFlush(game);
            moveRepository.saveAll(newMoves);
            return result;
        });

        List<Move> allMoves = new ArrayList<>(existing);
        allMoves.addAll(newMoves);
        return new GameState(saved, List.copyOf(allMoves), finalPosition);
    }

    private Game findGame(UUID id) {
        return gameRepository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND));
    }

    private static List<ChessMove> history(List<Move> moves) {
        return moves.stream().map(Move::toChessMove).toList();
    }

    static GameStatus toStatus(PositionOutcome outcome) {
        return switch (outcome) {
            case ONGOING -> GameStatus.PLAYING;
            case CHECKMATE -> GameStatus.CHECKMATE;
            case STALEMATE -> GameStatus.STALEMATE;
            case DRAW_REPETITION, DRAW_INSUFFICIENT_MATERIAL, DRAW_FIFTY_MOVE_RULE -> GameStatus.DRAW;
        };
    }
}
