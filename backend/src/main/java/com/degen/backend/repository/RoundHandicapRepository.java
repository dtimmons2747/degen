package com.degen.backend.repository;

import com.degen.backend.entity.RoundHandicap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundHandicapRepository extends JpaRepository<RoundHandicap, Long> {
  List<RoundHandicap> findByRoundTeeTimeId(Long roundTeeTimeId);

  Optional<RoundHandicap> findByRoundTeeTimeIdAndPlayerId(Long roundTeeTimeId, Long playerId);

  void deleteByRoundTeeTimeId(Long roundTeeTimeId);
}
