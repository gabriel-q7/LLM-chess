# Difficulty levels

A game has one level, chosen when it is created and stored with it (`games.difficulty`). The level
sets **two things together**: how strong the computer plays, and how much the AI assistant may
reveal. The harder the game, the stronger the opponent and the less help.

The mapping lives in `Difficulty` (`backend/…/game/domain/Difficulty.java`).

## Opponent strength

| Level | Stockfish Skill Level | Search depth |
|---|---|---|
| Easy | 2 | 4 |
| Medium | 8 | 8 |
| Hard | 20 (full strength) | 14 |

Stockfish's *Skill Level* deliberately picks weaker moves with some randomness. Combined with a
shallow search, Easy makes human-like mistakes. The analysis the assistant explains is **always**
full strength (`STOCKFISH_ANALYSIS_DEPTH`, default 18), whatever the level.

## What the assistant may reveal

Each level maps to a `Disclosure`:

| | Easy (`FULL`) | Medium (`HINTS`) | Hard (`MINIMAL`) |
|---|---|---|---|
| Exact best move | yes | no | no |
| Piece hint ("consider a move with your knight") | — | yes | no |
| Engine continuation | yes | no | no |
| Evaluation | number and depth (`+0.40 at depth 18`) | words only ("White is slightly better") | words only |
| Check, mate threatened **against** the player | yes | yes | yes |
| Forced mate **for** the player | "mate in N" and the move | "you have a forced mate: look for it!" | not revealed (only "White is winning") |
| `bestMove` / `evaluation` / `mateIn` in `/analysis` | returned | `null` | `null` |

Asking "What should I play?" gives:

- **Easy:** "Stockfish's best move for you is d4. The expected continuation is d4 d5 Nc3 Nf6."
- **Medium:** "Hint: consider a move with your knight."
- **Hard:** "No hints at this level: look for your own threats and your opponent's."

## How hiding works

Filtering happens **before** anything reaches Laya. `PromptBuilder.facts()` builds a
`PositionFacts` that contains only disclosed facts (undisclosed ones are `null` or empty). Both the
text sent to Laya and the reply are built from it. The assistant therefore never receives the best
move, the line or the score on levels that hide them, so it cannot leak them. The FEN is not sent
to Laya on any level.

For example, this is the whole position context Laya receives on Hard:

```text
Facts about the chess position.
Move 2, opening. White to move.
Last move: e5
Material: equal
Assessment: White is slightly better
```

`GameAnalysisService` applies the same rule to the REST response. The frontend hides the
*Best move* and *Evaluation* tiles when those fields are `null`.

## Existing games

Databases created before levels existed get the column on start-up (`SchemaUpgrade`). Their games
become **Medium**.

## Changing the levels

Edit the `Difficulty` constants. New disclosure combinations are `Disclosure` records;
`LayaAiServiceTest` has one test per level asserting what the reply and the Laya state may and may
not contain.
