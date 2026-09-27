package com.example.chess.game.domain;

import com.example.chess.chess.Color;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "initial_fen", nullable = false)
    private String initialFen;

    @Column(nullable = false)
    private String fen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "player_color", nullable = false)
    private Color playerColor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Game() {
    }

    public Game(UUID id, String initialFen, Color playerColor, Difficulty difficulty, LocalDateTime now) {
        this.id = id;
        this.initialFen = initialFen;
        this.fen = initialFen;
        this.status = GameStatus.PLAYING;
        this.playerColor = playerColor;
        this.difficulty = difficulty;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updatePosition(String fen, GameStatus status, LocalDateTime now) {
        this.fen = fen;
        this.status = status;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getInitialFen() {
        return initialFen;
    }

    public String getFen() {
        return fen;
    }

    public GameStatus getStatus() {
        return status;
    }

    public Color getPlayerColor() {
        return playerColor;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Color getComputerColor() {
        return playerColor.opposite();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
