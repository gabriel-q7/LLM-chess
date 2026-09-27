# LLM-chess

A chess application where a human player plays against the computer and can ask an AI assistant
about the game. It has three difficulty levels. Stockfish computes the moves, chesslib enforces the
rules, and [Laya](https://nandhakishorm.github.io/laya/) powers the assistant.

## Quick start

```bash
docker compose -f infrastructure/docker-compose.yml up --build
```

Then open <http://localhost:4200>. The first run builds Laya from source and downloads its model;
see [Getting started](docs/getting-started.md).

## Documentation

See [docs/](docs/README.md): architecture, REST API, difficulty levels, AI assistant, chess engine,
configuration and testing.
