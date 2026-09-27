# Stockfish speaks UCI over stdin/stdout. socat exposes it on TCP: every connection gets its
# own Stockfish process, which exits when the connection closes.
FROM debian:trixie-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends stockfish socat \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10002 stockfish

USER stockfish
ENV STOCKFISH_PORT=4000
EXPOSE 4000

HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD echo uci | socat -T2 - TCP:127.0.0.1:${STOCKFISH_PORT} | grep -q uciok || exit 1

CMD ["sh", "-c", "exec socat TCP-LISTEN:${STOCKFISH_PORT},reuseaddr,fork EXEC:/usr/games/stockfish"]
