package com.example.chess.game.repository;

import com.example.chess.game.domain.Move;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MoveRepository extends JpaRepository<Move, UUID> {

    List<Move> findByGameIdOrderByPlyAsc(UUID gameId);
}
