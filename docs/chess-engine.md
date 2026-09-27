# Chess rules and engine

## Rules: chesslib

Rules come from [chesslib](https://github.com/bhlangonijr/chesslib) (`com.github.bhlangonijr:chesslib:1.3.7`,
published through JitPack).

**Why chesslib:**

- Pure Java with no native code; mature and maintained (1.3.7 released June 2026).
- Covers everything the game needs: legal move generation, check, checkmate, stalemate, castling,
  en passant, promotion, FEN in/out, SAN, repetition, insufficient material and the fifty-move
  counter.
- Fast enough to replay a whole game on every request.

`ChessService` is the only class that touches chesslib. The rest of the code uses the project's own
`ChessMove`, `ChessPosition`, `Color` and `PositionOutcome` types.

**How the rules are applied:**

- **Stateless replay.** Each call loads the game's initial FEN and replays its stored moves. The
  replayed board has the full history, so threefold repetition works.
- **Canonical FEN.** FENs are written with an en-passant square only when a capture is actually
  possible (`getFen(true, true)`).
- **Promotion.** Promotion without a piece becomes a queen. A promotion piece on a non-promotion
  move is rejected.

## Engine: Stockfish

The `ChessEngine` interface has two operations:

| Method | Used for | Strength |
|---|---|---|
| `getBestMove(fen, EngineStrength)` | The computer's move | Per [difficulty](difficulty-levels.md) |
| `analyze(fen)` | Analysis the AI explains | Full strength, `STOCKFISH_ANALYSIS_DEPTH` |

`StockfishService` normalises results to **White's point of view**:

- the evaluation is in pawns;
- `mateIn` is positive when White mates;
- forced mates set `evaluation` to ±100.

### UCI client

`StockfishClient` speaks UCI:

```text
uci → uciok
setoption name Skill Level value N
isready → readyok
ucinewgame
position fen <fen>
go depth D
… info depth … score cp|mate … pv …
bestmove <move> | bestmove (none)
```

- **Fresh session per search.** Every search opens a new session, so there is no shared state and
  no locking between requests.
- **Parsing.** Only primary-line `info` lines with an exact score are kept; `lowerbound` /
  `upperbound` lines are ignored. `bestmove (none)` (mate or stalemate) gives `bestMove = null`.
- **Timeouts.** Socket timeouts use `STOCKFISH_TIMEOUT`.
- **Failures** become `ENGINE_UNAVAILABLE`.

### Transport

The client supports two transports:

- **TCP (default):** connect to `STOCKFISH_HOST:STOCKFISH_PORT`.
- **Local process:** when `STOCKFISH_PATH` is set, start that binary and use its stdin/stdout.

### Stockfish container

`infrastructure/stockfish/Dockerfile` installs Debian trixie's `stockfish` (17.1) and `socat`:

```text
socat TCP-LISTEN:4000,reuseaddr,fork EXEC:/usr/games/stockfish
```

Each TCP connection gets its own Stockfish process, which exits when the connection closes. The
health check sends `uci` and expects `uciok`. The container runs as an unprivileged user.
