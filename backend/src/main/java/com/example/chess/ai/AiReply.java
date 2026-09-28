package com.example.chess.ai;

/**
 * The assistant's text and the trace of how it was produced.
 */
public record AiReply(String text, AiTrace trace) {
}
