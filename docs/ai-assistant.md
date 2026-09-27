# AI assistant (Laya)

## What Laya is

[Laya](https://nandhakishorm.github.io/laya/) is a **non-autoregressive decision model**, not a
chat LLM. Given a text `state` and a set of typed questions, it returns one answer per question:

- `choice`: pick one option from a list, with probabilities;
- `score`: a level on a scale;
- `noul`: a yes/no probability.

It does not generate free text. Its HTTP server, `laya-serve`, exposes `POST /v1/systemone` and
`GET /health`, and **no OpenAI- or Ollama-compatible endpoint**.

Consequences for this project:

- **No Spring AI.** Spring AI has no model integration that fits this protocol. Following the
  project brief ("if it is not Ollama-compatible, create a small adapter"), the backend talks to
  Laya through `LayaClient`, a `RestClient`-based adapter behind the `AiService` interface.
  Callers do not depend on the protocol.
- **Replies are composed, not generated.** Laya makes the decisions (what the player asks about,
  which theme matters). `ExplanationComposer` writes the sentences from the engine facts the level
  discloses, so a reply can never mention a move Stockfish did not produce.

## Request flow

**Explain position.** One Laya request:

- `state`: the disclosed position facts as plain English (see `PromptBuilder.describe`);
- question `focus` (`choice`): development, attack, material, defense or endgame.

**Ask AI.** Two Laya requests:

1. `state` = `"A chess player asks: <question>"`, question `intent` (`choice`): `best_move`,
   `evaluation`, `last_move`, `threats`, `plan` or `other`. The question is classified **on its
   own**; with engine facts in the same state, Laya tended to classify the facts instead of the
   question.
2. The same `focus` request as above.

The composer then answers by intent. For example, `evaluation` returns the assessment and material;
`threats` returns check and mate threats; `other` says the assistant only discusses the game.

## Confidence gate

Each decision is used only when Laya's probability for the chosen option reaches
`LAYA_MIN_CONFIDENCE` (default `0.4`). Below that:

- an uncertain `intent` falls back to a short summary;
- an uncertain `focus` omits the theme sentence.

Observed during development:

- **Question intent is reliable.** 8 of 9 English test questions were classified correctly, mostly
  with high confidence.
- **Theme judgement is weak.** Laya cannot read a chess position, so `focus` is often near-uniform
  and sometimes wrong (for example "material" on move 2). The gate keeps most of those out; raise
  the threshold if themes appear where they should not.
- **Non-English questions** route best with `LAYA_MODEL=auto`, which downloads the multilingual
  checkpoint too. The default `english` checkpoint misclassified a Spanish question.

## What Laya sees

Only the facts the game's difficulty allows. See
[Difficulty levels](difficulty-levels.md#how-hiding-works). Set `AI_LOG_LEVEL=DEBUG` (Docker) or use
the `dev` profile (local) to log every `state` sent to Laya.

## Failure handling

Timeouts, connection errors and non-2xx responses become `AiServiceUnavailableException` →
`503 AI_SERVICE_UNAVAILABLE`. The first request after a cold start can be slow while a checkpoint
loads. `LAYA_PRELOAD=1` in Compose loads it at start-up, and `LAYA_TIMEOUT` defaults to 120 s.

## Running Laya

Compose builds Laya's own Dockerfile from `https://github.com/NandhaKishorM/laya.git#v0.3.9` and
runs `laya-serve` with `LAYA_MODELS=english` and `LAYA_PRELOAD=1`. The port is not published,
because the API has no authentication unless `LAYA_API_KEY` is set. If you set it, the backend
sends it as a bearer token.

Useful checks from inside the Compose network:

```bash
docker compose -f infrastructure/docker-compose.yml exec laya \
  python -c "import urllib.request;print(urllib.request.urlopen('http://127.0.0.1:8000/health').read())"
```
