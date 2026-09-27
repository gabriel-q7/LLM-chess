package com.example.chess.engine;

import com.example.chess.chess.ChessService;
import com.example.chess.chess.Color;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockfishService implements ChessEngine {

    private static final int FULL_STRENGTH = 20;

    private final StockfishClient client;
    private final ChessService chessService;
    private final EngineProperties properties;

    public StockfishService(StockfishClient client, ChessService chessService, EngineProperties properties) {
        this.client = client;
        this.chessService = chessService;
        this.properties = properties;
    }

    @Override
    public EngineAnalysis analyze(String fen) {
        StockfishClient.UciResult result = client.search(fen, properties.analysisDepth(), FULL_STRENGTH);
        return toWhitePerspective(fen, result);
    }

    @Override
    public String getBestMove(String fen) {
        StockfishClient.UciResult result = client.search(fen, properties.moveDepth(), properties.skillLevel());
        if (result.bestMove() == null) {
            throw new EngineUnavailableException("Engine returned no move for a position with legal moves");
        }
        return result.bestMove();
    }

    private EngineAnalysis toWhitePerspective(String fen, StockfishClient.UciResult result) {
        int sign = chessService.sideToMove(fen) == Color.WHITE ? 1 : -1;
        Integer mateIn = result.mateIn() == null ? null : sign * result.mateIn();
        double evaluation;
        if (mateIn != null) {
            // "mate 0" means the side to move is already mated.
            evaluation = (mateIn > 0 || (mateIn == 0 && sign < 0)) ? EngineAnalysis.MATE_SCORE : -EngineAnalysis.MATE_SCORE;
        } else {
            evaluation = sign * (result.scoreCp() == null ? 0 : result.scoreCp()) / 100.0;
        }
        List<String> pv = result.pv().isEmpty() && result.bestMove() != null ? List.of(result.bestMove()) : result.pv();
        return new EngineAnalysis(result.bestMove(), evaluation, result.depth(), mateIn, pv);
    }
}
