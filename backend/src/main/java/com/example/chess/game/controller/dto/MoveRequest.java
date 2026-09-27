package com.example.chess.game.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MoveRequest(
        @NotBlank @Pattern(regexp = "^[a-hA-H][1-8]$", message = "must be a square such as e2") String from,
        @NotBlank @Pattern(regexp = "^[a-hA-H][1-8]$", message = "must be a square such as e4") String to,
        @Pattern(regexp = "^[qrbnQRBN]$", message = "must be one of q, r, b, n") String promotion
) {
}
