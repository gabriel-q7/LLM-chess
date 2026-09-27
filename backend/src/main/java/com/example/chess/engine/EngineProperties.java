package com.example.chess.engine;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Stockfish connection settings. When {@code path} is set the engine runs as a local process;
 * otherwise the backend connects to {@code host}:{@code port}, where a TCP bridge speaks UCI.
 */
@Validated
@ConfigurationProperties(prefix = "chess.engine")
public record EngineProperties(
        String host,
        @Min(1) @Max(65535) int port,
        String path,
        @Min(1) @Max(40) int moveDepth,
        @Min(1) @Max(40) int analysisDepth,
        @Min(0) @Max(20) int skillLevel,
        @NotNull Duration timeout
) {

    public boolean useLocalProcess() {
        return path != null && !path.isBlank();
    }
}
