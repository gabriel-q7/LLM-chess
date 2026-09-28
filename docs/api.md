# REST API

Base path: `/api/games`. All bodies are JSON. Squares use lowercase algebraic coordinates (`e2`).
Colours are `WHITE` / `BLACK`.

## Create a game

`POST /api/games` → `201 Created`

```json
{ "playerColor": "WHITE", "difficulty": "MEDIUM" }
```

Both fields are optional: `playerColor` defaults to `WHITE`, `difficulty` to `MEDIUM`
(`EASY` | `MEDIUM` | `HARD`). When the player chooses Black, the computer's first move is already
in the response.

## Get a game

`GET /api/games/{id}` → `200 OK` with a game (below).

## Make a move

`POST /api/games/{id}/moves` → `200 OK` with the updated game, including the computer's reply.

```json
{ "from": "e7", "to": "e8", "promotion": "n" }
```

`promotion` (`q` | `r` | `b` | `n`) is only allowed on promotion moves, and defaults to a queen there.

## Game response

```json
{
  "id": "784ba19a-a175-4248-85b9-3df427b99a96",
  "fen": "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
  "status": "PLAYING",
  "turn": "WHITE",
  "playerColor": "WHITE",
  "difficulty": "MEDIUM",
  "check": false,
  "winner": null,
  "drawReason": null,
  "moves": [
    { "ply": 1, "moveNumber": 1, "color": "WHITE", "from": "e2", "to": "e4", "promotion": null, "san": "e4", "fen": "…" },
    { "ply": 2, "moveNumber": 1, "color": "BLACK", "from": "e7", "to": "e5", "promotion": null, "san": "e5", "fen": "…" }
  ],
  "legalMoves": [ { "from": "g1", "to": "f3", "promotion": null } ]
}
```

| Field | Meaning |
|---|---|
| `status` | `PLAYING`, `CHECKMATE`, `STALEMATE` or `DRAW` |
| `winner` | Winning colour after checkmate, else `null` |
| `drawReason` | `STALEMATE`, `REPETITION`, `INSUFFICIENT_MATERIAL`, `FIFTY_MOVE_RULE`, else `null` |
| `legalMoves` | The player's legal moves, computed by the backend; empty when it is not their turn or the game is over |
| `moves[].fen` | Position after that move |

## Analyse the position

`POST /api/games/{id}/analysis` → `200 OK`

```json
{ "bestMove": "d4", "evaluation": 0.4, "depth": 18, "mateIn": null,
  "explanation": "After e6, White is slightly better (+0.40 at depth 18). Stockfish's best move for you is d4. …" }
```

`evaluation` is in pawns from White's point of view. `mateIn` is positive when White mates. On
Medium and Hard, `bestMove`, `evaluation` and `mateIn` are `null`; see
[Difficulty levels](difficulty-levels.md).

Both `/analysis` and `/chat` also return a `trace`: what the assistant received and decided. It
contains only facts the level discloses, so it is safe on any level.

```json
"trace": {
  "disclosure": { "bestMove": false, "pieceHint": false, "continuation": false, "numericEvaluation": false, "playerMate": "HIDDEN" },
  "hiddenFacts": ["best move", "engine continuation", "numeric evaluation and depth", "the player's forced mate", "FEN"],
  "exchanges": [
    { "model": "english",
      "state": "A chess player asks: Who is winning?",
      "questions": { "intent": { "type": "choice", "instructions": "What is the player asking about?", "criteria": { "evaluation": "who is winning, the score or advantage" } } },
      "decisions": { "intent": { "choice": "evaluation", "probability": 0.77, "used": true, "probabilities": { "evaluation": 0.77, "other": 0.14 } } } }
  ]
}
```

`used` is `false` when the probability is below `LAYA_MIN_CONFIDENCE`, in which case the reply
ignored that decision.

## Ask the assistant

`POST /api/games/{id}/chat` → `200 OK`

```json
{ "question": "Who is winning?" }
```

```json
{ "answer": "White is slightly better. Material: equal." }
```

`question` is required, up to 500 characters. The response also carries a `trace` (see above).

## Engine context of the last computer move

`GET /api/games/{id}/engine-context` → `200 OK`, or `204 No Content` before the computer has moved.

This is a **debug endpoint**: it returns Stockfish's full output, including evaluation and line,
whatever the difficulty.

```json
{
  "ply": 2, "san": "e5",
  "fen": "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
  "difficulty": "HARD",
  "engine": {
    "move": "e7e5", "strength": { "skillLevel": 20, "depth": 14 }, "depth": 14,
    "evaluation": 0.30, "mateIn": null, "principalVariation": ["e7e5", "g1f3", "…"],
    "uciCommands": ["uci", "setoption name Skill Level value 20", "isready", "ucinewgame", "position fen …", "go depth 14", "quit"],
    "finalInfo": "info depth 14 seldepth 19 multipv 1 score cp -30 … pv e7e5 g1f3 …",
    "bestMoveLine": "bestmove e7e5 ponder g1f3"
  },
  "principalVariationSan": ["e5", "Nf3", "…"]
}
```

The context is stored with each computer move (`moves.engine_context`, JSON).

## Errors

Every error has the same shape. Messages are safe to show and never contain stack traces or
infrastructure details.

```json
{ "code": "ILLEGAL_MOVE", "message": "That move is not legal in the current position." }
```

| Code | HTTP | When |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Malformed body, bad square/promotion, empty question |
| `INVALID_POSITION` | 400 | A stored or supplied FEN cannot be loaded |
| `GAME_NOT_FOUND` | 404 | Unknown game id |
| `NOT_YOUR_TURN` | 409 | The computer is to move |
| `GAME_ALREADY_FINISHED` | 409 | Move after checkmate, stalemate or draw |
| `CONCURRENT_UPDATE` | 409 | Another request changed the game at the same time |
| `ILLEGAL_MOVE` | 422 | The move breaks the rules |
| `ENGINE_UNAVAILABLE` | 503 | Stockfish unreachable, timed out or returned no/illegal move |
| `AI_SERVICE_UNAVAILABLE` | 503 | Laya unreachable, timed out or failed |
| `INTERNAL_ERROR` | 500 | Anything unexpected (details are only in the server log) |

## Operations

`GET /actuator/health` and `GET /actuator/info` are exposed on the backend port.
