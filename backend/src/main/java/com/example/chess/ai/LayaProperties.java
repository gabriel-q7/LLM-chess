package com.example.chess.ai;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Laya server settings.
 *
 * @param baseUrl       laya-serve base URL, e.g. {@code http://laya:8000}
 * @param model         checkpoint sent in each request ({@code english}, {@code multilingual}, or {@code auto} to let Laya route)
 * @param apiKey        bearer token when the server sets {@code LAYA_API_KEY}; empty for none
 * @param timeout       read timeout; the first request after start-up may load a checkpoint
 * @param minConfidence probability Laya's answer must reach before the reply relies on it
 */
@Validated
@ConfigurationProperties(prefix = "chess.ai.laya")
public record LayaProperties(
        @NotBlank String baseUrl,
        @NotBlank String model,
        String apiKey,
        @NotNull Duration timeout,
        @DecimalMin("0.0") @DecimalMax("1.0") double minConfidence
) {
}
