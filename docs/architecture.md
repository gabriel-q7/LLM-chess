# Architecture

## Principle

Each concern has exactly one owner:

| Concern | Owner | Never done by |
|---|---|---|
| Legal moves, check, mate, draws, FEN, SAN | `ChessService` (chesslib) | frontend, Stockfish, Laya |
| Computer moves, evaluation, best move | Stockfish via `ChessEngine` | Laya |
| Understanding the player's question, choosing what to emphasise | Laya via `AiService` | — |
| Deciding what the player may learn | `Difficulty` → `Disclosure` | Laya |
| Persistence | Spring Data JPA on SQLite | — |
| Presentation | Angular | — |

The frontend never evaluates chess rules. It receives the list of legal moves from the backend and
only lets the player pick among them.

```text
Angular (4200) ── REST ──▶ Spring Boot (8080)
                             │
               ┌─────────────┼──────────────┐
               ▼             ▼              ▼
          ChessService   ChessEngine      AiService
          (chesslib)         │              │
                             ▼              ▼
                       Stockfish (TCP)  Laya (HTTP /v1/systemone)
                             │
                           SQLite
```

## Backend packages

`backend/src/main/java/com/example/chess/`

| Package | Contents |
|---|---|
| `game.controller` | `GameController` (thin) and request/response DTOs |
| `game.service` | `GameService` (game flow), `GameAnalysisService` (analysis and chat), `GameState` |
| `game.domain` | `Game`, `Move`, `GameStatus`, `Difficulty` |
| `game.repository` | `GameRepository`, `MoveRepository` |
| `chess` | `ChessService` and library-independent types (`ChessMove`, `ChessPosition`, `Color`, …) |
| `engine` | `ChessEngine` interface, `StockfishService`, `StockfishClient` (UCI), `EngineStrength` |
| `ai` | `AiService` interface, `LayaAiService`, `LayaClient`, `PromptBuilder`, `ExplanationComposer`, `Disclosure` |
| `common` | Error codes, `GlobalExceptionHandler`, `SchemaUpgrade` |

Controllers only map HTTP to service calls. External systems sit behind interfaces
(`ChessEngine`, `AiService`), and tests mock them.

## Making a move

`POST /api/games/{id}/moves` → `GameService.makeMove`:

1. Load the game and its moves. Reject the move if the game is finished (`GAME_ALREADY_FINISHED`)
   or it is not the player's turn (`NOT_YOUR_TURN`).
2. **Replay** the stored moves from the initial FEN with chesslib. Replaying keeps the full history,
   so threefold repetition is detected.
3. Validate and apply the player's move (`ILLEGAL_MOVE` if not legal).
4. If the game is still running, ask Stockfish for a reply at the game's
   [difficulty](difficulty-levels.md) strength and apply it. An illegal engine move is treated as
   `ENGINE_UNAVAILABLE`.
5. Persist both moves and the new FEN/status **in one transaction**.

Stockfish is consulted before anything is written. If it fails, nothing is stored and the player
can retry the same move. The game has an optimistic-lock `version`, so two concurrent moves on the
same game cannot both succeed (`CONCURRENT_UPDATE`).

## Analysis and chat

`POST /api/games/{id}/analysis` and `/chat` → `GameAnalysisService`:

1. Stockfish analyses the current position at full strength (`STOCKFISH_ANALYSIS_DEPTH`).
2. The game's `Difficulty` gives a `Disclosure`, which says what the player may learn.
3. `AiService` receives a `PositionContext` (FEN, last move, analysis, player colour, disclosure) and
   returns text. See [AI assistant](ai-assistant.md).
4. The response hides the engine fields the disclosure does not allow (`bestMove`, `evaluation` and
   `mateIn` become `null`).

## Persistence

SQLite at `data/chess.db`, accessed through Spring Data JPA with Hibernate's community
`SQLiteDialect`.

- **Schema:** `schema.sql` creates the tables (`CREATE TABLE IF NOT EXISTS`). Hibernate DDL is off.
- **Upgrades:** `SchemaUpgrade` adds columns that later versions introduced (currently
  `games.difficulty`) to databases created by earlier versions.
- **Connections:** one pooled connection, WAL journal and a 5 s busy timeout. SQLite allows a single
  writer, so this avoids `SQLITE_BUSY`.
- **IDs:** stored as text.

| Table | Key columns |
|---|---|
| `games` | `id`, `initial_fen`, `fen`, `status`, `player_color`, `difficulty`, `version`, timestamps |
| `moves` | `id`, `game_id`, `ply` (order), `move_number` (as in notation), `color`, `from_square`, `to_square`, `promotion`, `san`, `fen` (after the move) |

## Frontend

`frontend/src/app/`

| Path | Role |
|---|---|
| `core/models/` | API contracts (`Game`, `Move`, `Analysis`, …) |
| `core/services/game.service.ts` | Game HTTP calls and current game state (signals); remembers the game id in `localStorage` |
| `core/services/ai.service.ts` | Analysis and chat HTTP calls |
| `core/services/error-message.ts` | Maps API error codes to messages for the player |
| `features/game/` | Page: header, level selector, board, game info, move history |
| `features/chess-board/` | Board rendering and move selection from backend-supplied legal moves |
| `features/ai-chat/` | *Explain position* and *Ask AI* panel |

Components never call `HttpClient` directly. Styling uses Tailwind utility classes.
