package com.example.chess.engine;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Speaks the UCI protocol with Stockfish. Each search uses a fresh engine session — a new TCP
 * connection (the bridge starts one Stockfish process per connection) or a new local process —
 * so searches never share state and concurrent requests do not interfere.
 */
@Component
public class StockfishClient {

    private final EngineProperties properties;

    public StockfishClient(EngineProperties properties) {
        this.properties = properties;
    }

    /**
     * Searches {@code fen} to {@code depth} plies and returns the raw result, with scores from the
     * side to move's point of view as UCI reports them.
     */
    public UciResult search(String fen, int depth, int skillLevel) {
        try (Session session = open()) {
            session.send("uci");
            session.readUntil("uciok");
            session.send("setoption name Skill Level value " + skillLevel);
            session.send("isready");
            session.readUntil("readyok");
            session.send("ucinewgame");
            session.send("position fen " + fen);
            session.send("go depth " + depth);

            UciResult.Builder result = new UciResult.Builder();
            String line;
            while ((line = session.readLine()) != null) {
                if (line.startsWith("info ")) {
                    result.acceptInfo(line);
                } else if (line.startsWith("bestmove")) {
                    result.acceptBestMove(line);
                    session.send("quit");
                    return result.build();
                }
            }
            throw new EngineUnavailableException("Engine closed the connection before returning a move");
        } catch (SocketTimeoutException e) {
            throw new EngineUnavailableException("Engine did not answer within " + properties.timeout(), e);
        } catch (IOException e) {
            throw new EngineUnavailableException("Cannot communicate with the engine", e);
        }
    }

    private Session open() throws IOException {
        int timeoutMillis = Math.toIntExact(properties.timeout().toMillis());
        if (properties.useLocalProcess()) {
            Process process = new ProcessBuilder(properties.path()).redirectErrorStream(true).start();
            return new Session(process.getInputStream(), process.getOutputStream(), process::destroy);
        }
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(properties.host(), properties.port()), timeoutMillis);
        socket.setSoTimeout(timeoutMillis);
        return new Session(socket.getInputStream(), socket.getOutputStream(), socket::close);
    }

    private static final class Session implements AutoCloseable {

        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final Closer closer;

        Session(InputStream in, OutputStream out, Closer closer) {
            this.reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));
            this.writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.US_ASCII));
            this.closer = closer;
        }

        void send(String command) throws IOException {
            writer.write(command);
            writer.write('\n');
            writer.flush();
        }

        String readLine() throws IOException {
            return reader.readLine();
        }

        void readUntil(String expected) throws IOException {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals(expected)) {
                    return;
                }
            }
            throw new EngineUnavailableException("Engine closed the connection while waiting for " + expected);
        }

        @Override
        public void close() throws IOException {
            closer.close();
        }
    }

    @FunctionalInterface
    private interface Closer {
        void close() throws IOException;
    }

    /**
     * Result of one UCI search.
     *
     * @param bestMove     UCI move, or {@code null} for {@code bestmove (none)}
     * @param depth        deepest completed depth reported
     * @param scoreCp      centipawn score from the side to move's view, or {@code null} for mate scores
     * @param mateIn       moves to mate from the side to move's view, or {@code null}
     * @param pv           principal variation in UCI notation
     */
    public record UciResult(String bestMove, int depth, Integer scoreCp, Integer mateIn, List<String> pv) {

        static final class Builder {
            private String bestMove;
            private int depth;
            private Integer scoreCp;
            private Integer mateIn;
            private List<String> pv = List.of();

            void acceptInfo(String line) {
                String[] tokens = line.trim().split("\\s+");
                // Only primary-line updates carrying a score; skip "info string" and currmove chatter.
                if (!Arrays.asList(tokens).contains("score")) {
                    return;
                }
                int multipv = 1;
                Integer lineDepth = null;
                Integer cp = null;
                Integer mate = null;
                List<String> linePv = new ArrayList<>();
                for (int i = 1; i < tokens.length; i++) {
                    switch (tokens[i]) {
                        case "depth" -> lineDepth = Integer.parseInt(tokens[++i]);
                        case "multipv" -> multipv = Integer.parseInt(tokens[++i]);
                        case "score" -> {
                            String kind = tokens[++i];
                            int value = Integer.parseInt(tokens[++i]);
                            if (kind.equals("cp")) {
                                cp = value;
                            } else if (kind.equals("mate")) {
                                mate = value;
                            }
                        }
                        case "upperbound", "lowerbound" -> {
                            return; // aspiration-window bound, not an exact score
                        }
                        case "pv" -> {
                            linePv.addAll(Arrays.asList(tokens).subList(i + 1, tokens.length));
                            i = tokens.length;
                        }
                        default -> {
                        }
                    }
                }
                if (multipv != 1 || (cp == null && mate == null)) {
                    return;
                }
                if (lineDepth != null) {
                    depth = lineDepth;
                }
                scoreCp = cp;
                mateIn = mate;
                if (!linePv.isEmpty()) {
                    pv = List.copyOf(linePv);
                }
            }

            void acceptBestMove(String line) {
                String[] tokens = line.trim().split("\\s+");
                bestMove = tokens.length > 1 && !tokens[1].equals("(none)") ? tokens[1] : null;
            }

            UciResult build() {
                return new UciResult(bestMove, depth, scoreCp, mateIn, pv);
            }
        }
    }
}
