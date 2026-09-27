# Configuration

All infrastructure settings are environment variables with defaults in
`backend/src/main/resources/application.yml`. Nothing secret is committed. For Compose, put local
overrides in `infrastructure/.env` (Compose reads `.env` from the compose file's directory; the file
is git-ignored) or export them in your shell.

## Backend

| Variable | Default | Purpose |
|---|---|---|
| `DATABASE_PATH` | `./data/chess.db` (`../data/chess.db` in `dev`) | SQLite file; its directory is created on start-up |
| `STOCKFISH_HOST` | `localhost` | Stockfish TCP host (`stockfish` in Compose) |
| `STOCKFISH_PORT` | `4000` | Stockfish TCP port |
| `STOCKFISH_PATH` | empty | Path to a local Stockfish binary; when set, used instead of TCP |
| `STOCKFISH_ANALYSIS_DEPTH` | `18` | Depth of the full-strength analysis the AI explains |
| `STOCKFISH_TIMEOUT` | `30s` | Connect/read timeout per search |
| `LAYA_BASE_URL` | `http://localhost:8000` | Laya server (`http://laya:8000` in Compose) |
| `LAYA_MODEL` | `english` | Checkpoint sent with each request (`english`, `multilingual`, or `auto`) |
| `LAYA_API_KEY` | empty | Bearer token when Laya requires one |
| `LAYA_TIMEOUT` | `120s` | Read timeout for Laya requests |
| `LAYA_MIN_CONFIDENCE` | `0.4` | Minimum probability before a Laya decision is used |
| `SERVER_PORT` | `8080` | HTTP port |

The computer's playing strength is not configured here; it comes from the game's
[difficulty](difficulty-levels.md).

## Docker Compose only

| Variable | Default | Purpose |
|---|---|---|
| `FRONTEND_PORT` | `4200` | Host port for the frontend |
| `BACKEND_PORT` | `8080` | Host port for the backend |
| `APP_UID` / `APP_GID` | `1000` | User the backend runs as, so `data/chess.db` stays owned by you |
| `AI_LOG_LEVEL` | `INFO` | `DEBUG` logs every state sent to Laya |
| `LAYA_MODELS` | `english` | Checkpoints Laya preloads (`english,multilingual,typed-decisions`) |
| `LAYA_THREADS` | `4` | CPU threads for Laya; keep at or below physical cores |
| `LAYA_DEVICE` | `cpu` | Torch device |
| `LAYA_TORCH_INDEX` / `LAYA_TORCH_VERSION` | `cpu` / `2.14.0` | PyTorch wheels used when building Laya |
| `HF_HUB_OFFLINE` | `0` | `1` uses only cached Laya weights |

## Frontend

| Variable | Default | Purpose |
|---|---|---|
| `BACKEND_URL` | `http://localhost:8080` | Target of the dev-server `/api` proxy (`frontend/proxy.conf.mjs`); Compose sets `http://backend:8080` |

## Spring profiles

| Profile | Effect |
|---|---|
| default | Settings above; used in Docker |
| `dev` | Database at `../data/chess.db` (run from `backend/`), DEBUG logging for `com.example.chess` |
