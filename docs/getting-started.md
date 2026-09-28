# Getting started

## Prerequisites

| To run… | You need |
|---|---|
| The full stack in Docker | Docker Engine with Compose v2+, ~8 GB RAM, ~10 GB free disk (Laya) |
| The backend locally | Java 21 (the Maven wrapper is included) |
| The frontend locally | Node.js 22+ and npm 11+ |

## Run everything with Docker Compose

From the repository root:

```bash
docker compose -f infrastructure/docker-compose.yml up --build
```

Open <http://localhost:4200>.

| Service | Purpose | Host port |
|---|---|---|
| `frontend` | Angular dev server; forwards `/api` to the backend | 4200 |
| `backend` | Spring Boot REST API | 8080 |
| `stockfish` | Stockfish over TCP (UCI) | not published |
| `laya` | Laya HTTP server (`laya-serve`) | not published |

The services reach each other by service name on the Compose network (`http://backend:8080`,
`http://laya:8000`, `stockfish:4000`), never `localhost`.

### First run

- **Laya has no published image.** Compose builds it from the official repository at tag `v0.3.9`
  (PyTorch CPU wheels). The first build takes several minutes.
- **Model weights download on first start.** On start-up Laya downloads its `english` checkpoint
  from Hugging Face. The weights are kept in the `laya-model-cache` volume, so later starts reuse
  them.
- **Games work before Laya is ready.** Only *Explain position* and *Ask AI* need Laya. Until Laya
  reports healthy, those return `AI_SERVICE_UNAVAILABLE`.

### Everyday commands

```bash
# start in the background / stop
docker compose -f infrastructure/docker-compose.yml up -d
docker compose -f infrastructure/docker-compose.yml down

# follow logs (all services, or one)
docker compose -f infrastructure/docker-compose.yml logs -f
docker compose -f infrastructure/docker-compose.yml logs -f backend

# rebuild one service after code changes
docker compose -f infrastructure/docker-compose.yml up -d --build backend

# see the exact context sent to Laya
AI_LOG_LEVEL=DEBUG docker compose -f infrastructure/docker-compose.yml up -d backend

# delete Laya's downloaded weights (they download again next start)
docker compose -f infrastructure/docker-compose.yml down --volumes
```

The SQLite file is bind-mounted from `./data`. The backend container runs as UID/GID 1000 by
default so that the file stays owned by your user. If your UID differs, set `APP_UID` / `APP_GID`.

## Run locally (without Docker for the app)

Stockfish and Laya still run in containers; the backend and frontend run on your machine.

1. **Start the engine and the AI**, with their ports published to localhost:

   ```bash
   docker build -t chess-ai-stockfish infrastructure/stockfish
   docker run -d --name stockfish-dev -p 127.0.0.1:4000:4000 chess-ai-stockfish

   docker compose -f infrastructure/docker-compose.yml build laya
   docker run -d --name laya-dev -p 127.0.0.1:8000:8000 \
     -e LAYA_PRELOAD=1 -e LAYA_MODELS=english \
     -v chess-ai_laya-model-cache:/home/laya/.cache/huggingface \
     chess-ai-laya:0.3.9 laya-serve
   ```

   Instead of the Stockfish container you can point `STOCKFISH_PATH` at a local binary.

2. **Backend** (the `dev` profile uses `../data/chess.db` and DEBUG logging):

   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

3. **Frontend** (`proxy.conf.mjs` forwards `/api` to `http://localhost:8080`):

   ```bash
   cd frontend
   npm install
   npm start
   ```

   Open <http://localhost:4200>.

## Playing

1. Pick a level (**Easy**, **Medium** or **Hard**) and click **New game as White** or
   **New game as Black**.
2. Click a piece, then a highlighted square. The backend supplies the legal destinations. Promotions
   ask which piece to promote to.
3. The computer replies automatically.
4. **Explain position** asks for an AI explanation of the current position.
5. **Ask AI** accepts free-text questions ("Who is winning?", "What should I play?").
6. **AI context** (header) toggles a debug panel with exactly what Stockfish and Laya received. See
   [Difficulty levels](difficulty-levels.md#debug-panel).

The browser remembers the current game and reloads it after a page refresh.
