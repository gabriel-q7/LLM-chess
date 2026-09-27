package com.example.chess.game.domain;

import com.example.chess.chess.ChessMove;
import com.example.chess.chess.Color;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "moves")
public class Move {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID gameId;

    /** Half-move index within the game, starting at 1. Defines move order. */
    @Column(nullable = false)
    private int ply;

    /** Full-move number as written in chess notation (1. e4 e5 → both have move number 1). */
    @Column(name = "move_number", nullable = false)
    private int moveNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Color color;

    @Column(name = "from_square", nullable = false)
    private String fromSquare;

    @Column(name = "to_square", nullable = false)
    private String toSquare;

    private String promotion;

    @Column(nullable = false)
    private String san;

    /** Position after this move. */
    @Column(nullable = false)
    private String fen;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Move() {
    }

    public Move(UUID id, UUID gameId, int ply, int moveNumber, Color color, ChessMove move, String san,
                String fen, LocalDateTime createdAt) {
        this.id = id;
        this.gameId = gameId;
        this.ply = ply;
        this.moveNumber = moveNumber;
        this.color = color;
        this.fromSquare = move.from();
        this.toSquare = move.to();
        this.promotion = move.promotion();
        this.san = san;
        this.fen = fen;
        this.createdAt = createdAt;
    }

    public ChessMove toChessMove() {
        return new ChessMove(fromSquare, toSquare, promotion);
    }

    public UUID getId() {
        return id;
    }

    public UUID getGameId() {
        return gameId;
    }

    public int getPly() {
        return ply;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    public Color getColor() {
        return color;
    }

    public String getFromSquare() {
        return fromSquare;
    }

    public String getToSquare() {
        return toSquare;
    }

    public String getPromotion() {
        return promotion;
    }

    public String getSan() {
        return san;
    }

    public String getFen() {
        return fen;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
