package com.example.chess.chess;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A move expressed in coordinates, independent of the chess library.
 *
 * @param from      origin square, e.g. {@code e2}
 * @param to        destination square, e.g. {@code e4}
 * @param promotion promotion piece ({@code q}, {@code r}, {@code b}, {@code n}) or {@code null}
 */
public record ChessMove(String from, String to, String promotion) {

    private static final Pattern UCI = Pattern.compile("^[a-h][1-8][a-h][1-8][qrbn]?$");

    public ChessMove {
        from = from.toLowerCase(Locale.ROOT);
        to = to.toLowerCase(Locale.ROOT);
        promotion = promotion == null || promotion.isBlank() ? null : promotion.toLowerCase(Locale.ROOT);
    }

    public static ChessMove fromUci(String uci) {
        if (uci == null || !UCI.matcher(uci.toLowerCase(Locale.ROOT)).matches()) {
            throw new IllegalArgumentException("Not a UCI move: " + uci);
        }
        String promotion = uci.length() == 5 ? uci.substring(4) : null;
        return new ChessMove(uci.substring(0, 2), uci.substring(2, 4), promotion);
    }

    public String toUci() {
        return from + to + (promotion == null ? "" : promotion);
    }
}
