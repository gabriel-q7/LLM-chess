# AI Chess — Documentation

AI Chess is a web application where a human plays chess against the computer and can ask an AI
assistant about the game.

- **Rules and game state:** a Java chess library (chesslib) running in the backend.
- **Computer moves and analysis:** Stockfish.
- **Assistant:** Laya, a typed-decision model. It explains Stockfish's analysis within limits set by
  the game's difficulty.

| Document | What it covers |
|---|---|
| [Getting started](getting-started.md) | Running the stack with Docker Compose or locally, first-run notes |
| [Architecture](architecture.md) | Components, responsibilities, request flow, persistence |
| [REST API](api.md) | Endpoints, payloads and error codes |
| [Difficulty levels](difficulty-levels.md) | What Easy / Medium / Hard change for the opponent and the assistant |
| [AI assistant (Laya)](ai-assistant.md) | How Laya is integrated, what it sees, how replies are built |
| [Chess engine (Stockfish)](chess-engine.md) | Rules library, UCI integration, the Stockfish container |
| [Configuration](configuration.md) | Environment variables and Spring profiles |
| [Testing](testing.md) | Backend and frontend test suites, integration tests |

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | Angular 21 (standalone components, signals, zoneless), Tailwind CSS 4, Vitest |
| Backend | Java 21, Spring Boot 4.1, Spring Data JPA (Hibernate 7) |
| Chess rules | [chesslib](https://github.com/bhlangonijr/chesslib) 1.3.7 |
| Engine | Stockfish 17.1 (Debian package) behind a TCP bridge |
| AI model | [Laya](https://nandhakishorm.github.io/laya/) v0.3.9 served by `laya-serve` |
| Database | SQLite (`data/chess.db`) |
| Runtime | Docker Compose |

## Repository layout

```text
.
├── backend/          Spring Boot application (Maven)
├── frontend/         Angular application
├── infrastructure/   docker-compose.yml and the Stockfish image
├── data/             SQLite database (created at runtime)
└── docs/             This documentation
```
