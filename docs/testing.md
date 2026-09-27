# Testing

## Backend

```bash
cd backend
./mvnw test                                  # unit and Spring tests, no external services
STOCKFISH_HOST=localhost ./mvnw verify       # plus Stockfish integration tests
```

The ordinary suite needs neither Stockfish nor Laya: the engine and the AI are mocked.

| Test | Covers |
|---|---|
| `ChessServiceTest` | Pawn and knight moves, castling (and through attacked squares), en passant, promotion, check, checkmate, stalemate, repetition, insufficient material, SAN, invalid FEN |
| `GameServiceTest` | `@SpringBootTest` on a temporary SQLite file: create/retrieve, legal move with computer reply, illegal move, move after checkmate, engine failure leaves the game unchanged, difficulty persisted and used for engine strength, hidden analysis fields on Hard |
| `GameControllerTest` | `@WebMvcTest`: JSON shape, validation, error codes, no infrastructure details in errors |
| `StockfishServiceTest` | UCI output parsing, White-perspective normalisation, strength passed to the engine |
| `LayaClientTest` | Wire format of `/v1/systemone` against `MockRestServiceServer`, error wrapping |
| `LayaAiServiceTest` | Replies per level, and that the state sent to Laya omits undisclosed facts |
| `SchemaUpgradeTest` | Old databases gain `games.difficulty` |
| `StockfishServiceIT` | Real engine: legal move, mate in one, no move when mated |

Integration tests (`*IT`) run in Maven's `verify` phase (Failsafe). They are skipped unless
`STOCKFISH_HOST` is set. Start an engine first, e.g.
`docker compose -f infrastructure/docker-compose.yml up -d stockfish`. That service does not publish
its port, so for tests on the host use the standalone container from
[Getting started](getting-started.md#run-locally-without-docker-for-the-app).

## Frontend

```bash
cd frontend
npx ng test --watch=false
```

Vitest runs with Angular's test builder and `HttpTestingController`; no backend is needed.

| Spec | Covers |
|---|---|
| `chess-board.spec.ts` | 64 squares and the starting pieces, orientation, only backend-supplied destinations, move emission, promotion picker, disabled board |
| `game.spec.ts` | Game creation, level selector, move submission and history, error display, checkmate status |
| `ai-chat.spec.ts` | Analysis display, hidden engine fields, AI unavailable message, chat round trip |
| `game.service.spec.ts` / `ai.service.spec.ts` | HTTP calls, state updates, error messages |

## Manual end-to-end check

With the Compose stack running:

```bash
B=localhost:4200/api/games
ID=$(curl -s -XPOST $B -H 'content-type: application/json' -d '{"difficulty":"HARD"}' | jq -r .id)
curl -s -XPOST $B/$ID/moves -H 'content-type: application/json' -d '{"from":"e2","to":"e4"}' | jq '.moves[].san'
curl -s -XPOST $B/$ID/analysis | jq                       # bestMove/evaluation null on HARD
curl -s -XPOST $B/$ID/chat -H 'content-type: application/json' -d '{"question":"What should I play?"}' | jq
```
