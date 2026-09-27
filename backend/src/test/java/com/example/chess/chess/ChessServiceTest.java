package com.example.chess.chess;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.chess.chess.ChessService.STANDARD_START_FEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChessServiceTest {

    private final ChessService chess = new ChessService();

    private static List<ChessMove> moves(String... uci) {
        return java.util.Arrays.stream(uci).map(ChessMove::fromUci).toList();
    }

    @Test
    void initialPositionHasTwentyLegalMovesAndWhiteToMove() {
        ChessPosition position = chess.position(STANDARD_START_FEN, List.of());

        assertThat(position.turn()).isEqualTo(Color.WHITE);
        assertThat(position.legalMoves()).hasSize(20);
        assertThat(position.outcome()).isEqualTo(PositionOutcome.ONGOING);
        assertThat(position.check()).isFalse();
    }

    @Test
    void pawnMovesOneOrTwoSquaresFromStartButNotThree() {
        AppliedMove single = chess.applyMove(STANDARD_START_FEN, List.of(), ChessMove.fromUci("e2e3"));
        AppliedMove dbl = chess.applyMove(STANDARD_START_FEN, List.of(), ChessMove.fromUci("e2e4"));

        assertThat(single.san()).isEqualTo("e3");
        assertThat(dbl.san()).isEqualTo("e4");
        assertThat(dbl.position().fen()).startsWith("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq");
        assertThat(dbl.position().turn()).isEqualTo(Color.BLACK);
        assertThatThrownBy(() -> chess.applyMove(STANDARD_START_FEN, List.of(), ChessMove.fromUci("e2e5")))
                .isInstanceOf(IllegalMoveException.class);
    }

    @Test
    void pawnCannotMoveBackwards() {
        assertThatThrownBy(() -> chess.applyMove(STANDARD_START_FEN, moves("e2e4", "e7e5"), ChessMove.fromUci("e4e3")))
                .isInstanceOf(IllegalMoveException.class);
    }

    @Test
    void knightJumpsInLShapeOnly() {
        AppliedMove move = chess.applyMove(STANDARD_START_FEN, List.of(), ChessMove.fromUci("g1f3"));

        assertThat(move.san()).isEqualTo("Nf3");
        assertThatThrownBy(() -> chess.applyMove(STANDARD_START_FEN, List.of(), ChessMove.fromUci("g1g3")))
                .isInstanceOf(IllegalMoveException.class);
    }

    @Test
    void cannotMoveOpponentPieces() {
        assertThatThrownBy(() -> chess.applyMove(STANDARD_START_FEN, List.of(), ChessMove.fromUci("e7e5")))
                .isInstanceOf(IllegalMoveException.class);
    }

    @Test
    void kingsideCastlingMovesKingAndRook() {
        List<ChessMove> history = moves("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6");

        AppliedMove castle = chess.applyMove(STANDARD_START_FEN, history, ChessMove.fromUci("e1g1"));

        assertThat(castle.san()).isEqualTo("O-O");
        assertThat(castle.position().fen()).startsWith("r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQ1RK1 b kq");
    }

    @Test
    void cannotCastleThroughAttackedSquare() {
        String fen = "4k3/8/8/8/8/8/8/4K2R w K - 0 1";
        String attacked = "4kr2/8/8/8/8/8/8/4K2R w K - 0 1";

        assertThat(chess.applyMove(fen, List.of(), ChessMove.fromUci("e1g1")).san()).isEqualTo("O-O");
        assertThatThrownBy(() -> chess.applyMove(attacked, List.of(), ChessMove.fromUci("e1g1")))
                .isInstanceOf(IllegalMoveException.class);
    }

    @Test
    void enPassantCaptureRemovesThePassedPawn() {
        List<ChessMove> history = moves("e2e4", "a7a6", "e4e5", "d7d5");

        AppliedMove capture = chess.applyMove(STANDARD_START_FEN, history, ChessMove.fromUci("e5d6"));

        assertThat(capture.san()).isEqualTo("exd6");
        assertThat(capture.position().fen()).startsWith("rnbqkbnr/1pp1pppp/p2P4/8/8/8/PPPP1PPP/RNBQKBNR b KQkq");
    }

    @Test
    void enPassantIsOnlyAvailableImmediately() {
        List<ChessMove> history = moves("e2e4", "a7a6", "e4e5", "d7d5", "h2h3", "h7h6");

        assertThatThrownBy(() -> chess.applyMove(STANDARD_START_FEN, history, ChessMove.fromUci("e5d6")))
                .isInstanceOf(IllegalMoveException.class);
    }

    @Test
    void promotionDefaultsToQueenAndAllowsUnderPromotion() {
        String fen = "8/P7/8/8/8/8/8/k6K w - - 0 1";

        AppliedMove queen = chess.applyMove(fen, List.of(), new ChessMove("a7", "a8", null));
        AppliedMove knight = chess.applyMove(fen, List.of(), new ChessMove("a7", "a8", "n"));

        assertThat(queen.san()).startsWith("a8=Q");
        assertThat(queen.position().fen()).startsWith("Q7/8/8/8/8/8/8/k6K b");
        assertThat(knight.san()).isEqualTo("a8=N");
        assertThat(knight.move().promotion()).isEqualTo("n");
    }

    @Test
    void promotionPieceIsRejectedOnNormalMove() {
        assertThatThrownBy(() -> chess.applyMove(STANDARD_START_FEN, List.of(), new ChessMove("e2", "e4", "q")))
                .isInstanceOf(IllegalMoveException.class);
    }

    @Test
    void detectsCheck() {
        List<ChessMove> history = moves("e2e4", "f7f6", "d2d4", "g7g5");

        AppliedMove check = chess.applyMove(STANDARD_START_FEN, history, ChessMove.fromUci("d1h5"));

        assertThat(check.san()).isEqualTo("Qh5#");
        assertThat(check.position().check()).isTrue();
    }

    @Test
    void mustRespondToCheck() {
        List<ChessMove> history = moves("e2e4", "e7e5", "f1c4", "d7d6", "c4f7");

        ChessPosition position = chess.position(STANDARD_START_FEN, history);

        assertThat(position.check()).isTrue();
        assertThat(position.legalMoves()).containsExactlyInAnyOrder(ChessMove.fromUci("e8f7"), ChessMove.fromUci("e8e7"),
                ChessMove.fromUci("e8d7"));
    }

    @Test
    void detectsCheckmate() {
        List<ChessMove> foolsMate = moves("f2f3", "e7e5", "g2g4", "d8h4");

        ChessPosition position = chess.position(STANDARD_START_FEN, foolsMate);

        assertThat(position.outcome()).isEqualTo(PositionOutcome.CHECKMATE);
        assertThat(position.check()).isTrue();
        assertThat(position.legalMoves()).isEmpty();
    }

    @Test
    void detectsStalemate() {
        String fen = "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1";

        AppliedMove move = chess.applyMove(fen, List.of(), ChessMove.fromUci("g6h6"));

        assertThat(move.position().outcome()).isEqualTo(PositionOutcome.STALEMATE);
        assertThat(move.position().check()).isFalse();
    }

    @Test
    void detectsInsufficientMaterial() {
        String fen = "7k/8/8/8/8/8/6q1/K5B1 w - - 0 1";

        AppliedMove capture = chess.applyMove(fen, List.of(), ChessMove.fromUci("g1f2"));
        ChessPosition afterTrade = chess.position("7k/8/8/8/8/8/8/K5b1 w - - 0 1", List.of());

        assertThat(capture.position().outcome()).isEqualTo(PositionOutcome.ONGOING);
        assertThat(afterTrade.outcome()).isEqualTo(PositionOutcome.DRAW_INSUFFICIENT_MATERIAL);
    }

    @Test
    void detectsThreefoldRepetition() {
        List<ChessMove> shuffle = moves("g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8");

        ChessPosition position = chess.position(STANDARD_START_FEN, shuffle);

        assertThat(position.outcome()).isEqualTo(PositionOutcome.DRAW_REPETITION);
    }

    @Test
    void convertsUciLineToSan() {
        List<String> san = chess.toSanLine(STANDARD_START_FEN, List.of("e2e4", "e7e5", "g1f3", "b8c6", "zzzz"));

        assertThat(san).containsExactly("e4", "e5", "Nf3", "Nc6");
    }

    @Test
    void countsMaterial() {
        assertThat(chess.material(STANDARD_START_FEN, Color.WHITE)).isEqualTo(39);
        assertThat(chess.material(STANDARD_START_FEN, Color.BLACK)).isEqualTo(39);
    }

    @Test
    void rejectsInvalidFen() {
        assertThatThrownBy(() -> chess.position("not a fen", List.of()))
                .isInstanceOf(InvalidPositionException.class);
    }
}
