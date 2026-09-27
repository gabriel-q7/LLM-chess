CREATE TABLE IF NOT EXISTS games (
    id           VARCHAR(36)  PRIMARY KEY,
    initial_fen  VARCHAR(100) NOT NULL,
    fen          VARCHAR(100) NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    player_color VARCHAR(5)   NOT NULL,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS moves (
    id           VARCHAR(36)  PRIMARY KEY,
    game_id      VARCHAR(36)  NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    ply          INTEGER      NOT NULL,
    move_number  INTEGER      NOT NULL,
    color        VARCHAR(5)   NOT NULL,
    from_square  VARCHAR(2)   NOT NULL,
    to_square    VARCHAR(2)   NOT NULL,
    promotion    VARCHAR(1),
    san          VARCHAR(10)  NOT NULL,
    fen          VARCHAR(100) NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    UNIQUE (game_id, ply)
);

CREATE INDEX IF NOT EXISTS idx_moves_game_id ON moves (game_id);
