package com.example.chess.chess;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Chess rules backed by chesslib. Stateless: every call rebuilds the board by replaying the
 * game from its initial FEN, so repetition detection sees the full history.
 */
@Service
public class ChessService {

    public static final String STANDARD_START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public ChessPosition position(String initialFen, List<ChessMove> history) {
        return snapshot(replay(initialFen, history));
    }

    public AppliedMove applyMove(String initialFen, List<ChessMove> history, ChessMove move) {
        Board board = replay(initialFen, history);
        Move libMove = findLegalMove(board, move);
        Color color = toColor(board.getSideToMove());
        int moveNumber = board.getMoveCounter();
        String san = toSan(board.getFen(), libMove);
        board.doMove(libMove);
        return new AppliedMove(toChessMove(libMove), color, moveNumber, san, snapshot(board));
    }

    /** Converts a sequence of UCI moves played from {@code fen} into SAN. Stops at the first illegal move. */
    public List<String> toSanLine(String fen, List<String> uciMoves) {
        Board board = load(fen);
        List<String> san = new ArrayList<>();
        for (String uci : uciMoves) {
            Move libMove;
            try {
                libMove = findLegalMove(board, ChessMove.fromUci(uci));
            } catch (IllegalArgumentException | IllegalMoveException e) {
                break;
            }
            san.add(toSan(board.getFen(), libMove));
            board.doMove(libMove);
        }
        return san;
    }

    public Color sideToMove(String fen) {
        return toColor(load(fen).getSideToMove());
    }

    /** Material in conventional pawn units (P=1, N=B=3, R=5, Q=9). */
    public int material(String fen, Color color) {
        Board board = load(fen);
        Side side = color == Color.WHITE ? Side.WHITE : Side.BLACK;
        int total = 0;
        for (Square square : Square.values()) {
            Piece piece = square == Square.NONE ? Piece.NONE : board.getPiece(square);
            if (piece != Piece.NONE && piece.getPieceSide() == side) {
                total += switch (piece.getPieceType()) {
                    case PAWN -> 1;
                    case KNIGHT, BISHOP -> 3;
                    case ROOK -> 5;
                    case QUEEN -> 9;
                    default -> 0;
                };
            }
        }
        return total;
    }

    private Board replay(String initialFen, List<ChessMove> history) {
        Board board = load(initialFen);
        for (ChessMove move : history) {
            board.doMove(findLegalMove(board, move));
        }
        return board;
    }

    private Board load(String fen) {
        Board board = new Board();
        try {
            board.loadFromFen(fen);
            // chesslib accepts some malformed FENs silently; generating moves exposes most of them.
            board.legalMoves();
        } catch (RuntimeException e) {
            throw new InvalidPositionException("Invalid FEN: " + fen, e);
        }
        return board;
    }

    private Move findLegalMove(Board board, ChessMove move) {
        Square from = toSquare(move.from());
        Square to = toSquare(move.to());
        List<Move> candidates = board.legalMoves().stream()
                .filter(m -> m.getFrom() == from && m.getTo() == to)
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalMoveException("Illegal move " + move.from() + "-" + move.to());
        }
        boolean isPromotion = candidates.getFirst().getPromotion() != Piece.NONE;
        if (!isPromotion) {
            if (move.promotion() != null) {
                throw new IllegalMoveException("Move " + move.from() + "-" + move.to() + " is not a promotion");
            }
            return candidates.getFirst();
        }
        // Promotion defaults to a queen when the client does not choose.
        PieceType wanted = toPieceType(move.promotion() == null ? "q" : move.promotion());
        return candidates.stream()
                .filter(m -> m.getPromotion().getPieceType() == wanted)
                .findFirst()
                .orElseThrow(() -> new IllegalMoveException("Invalid promotion piece: " + move.promotion()));
    }

    private ChessPosition snapshot(Board board) {
        List<ChessMove> legal = board.legalMoves().stream().map(this::toChessMove).toList();
        return new ChessPosition(
                // Only record an en-passant square when a capture is actually possible.
                board.getFen(true, true),
                toColor(board.getSideToMove()),
                board.isKingAttacked(),
                outcome(board, legal.isEmpty()),
                legal);
    }

    private PositionOutcome outcome(Board board, boolean noLegalMoves) {
        if (noLegalMoves) {
            return board.isKingAttacked() ? PositionOutcome.CHECKMATE : PositionOutcome.STALEMATE;
        }
        if (board.isInsufficientMaterial()) {
            return PositionOutcome.DRAW_INSUFFICIENT_MATERIAL;
        }
        if (board.isRepetition()) {
            return PositionOutcome.DRAW_REPETITION;
        }
        if (board.getHalfMoveCounter() >= 100) {
            return PositionOutcome.DRAW_FIFTY_MOVE_RULE;
        }
        return PositionOutcome.ONGOING;
    }

    private String toSan(String fenBefore, Move move) {
        MoveList list = new MoveList(fenBefore);
        list.add(move);
        return list.toSanArray()[0];
    }

    private ChessMove toChessMove(Move move) {
        String promotion = move.getPromotion() == Piece.NONE
                ? null
                : switch (move.getPromotion().getPieceType()) {
                    case QUEEN -> "q";
                    case ROOK -> "r";
                    case BISHOP -> "b";
                    case KNIGHT -> "n";
                    default -> null;
                };
        return new ChessMove(
                move.getFrom().value().toLowerCase(Locale.ROOT),
                move.getTo().value().toLowerCase(Locale.ROOT),
                promotion);
    }

    private static Square toSquare(String square) {
        try {
            return Square.valueOf(square.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalMoveException("Invalid square: " + square);
        }
    }

    private static PieceType toPieceType(String promotion) {
        return switch (promotion) {
            case "q" -> PieceType.QUEEN;
            case "r" -> PieceType.ROOK;
            case "b" -> PieceType.BISHOP;
            case "n" -> PieceType.KNIGHT;
            default -> throw new IllegalMoveException("Invalid promotion piece: " + promotion);
        };
    }

    private static Color toColor(Side side) {
        return side == Side.WHITE ? Color.WHITE : Color.BLACK;
    }
}
