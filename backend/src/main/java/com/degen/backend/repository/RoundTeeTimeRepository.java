package com.degen.backend.repository;

import com.degen.backend.entity.RoundTeeTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundTeeTimeRepository extends JpaRepository<RoundTeeTime, Long> {
    @Query("SELECT r FROM RoundTeeTime r WHERE r.tournamentRound.id = :tournamentRoundId")
    Optional<RoundTeeTime> findByTournamentRoundId(@Param("tournamentRoundId") Long tournamentRoundId);
    
    @Query("SELECT r FROM RoundTeeTime r WHERE r.tournamentRound.id = :tournamentRoundId")
    List<RoundTeeTime> findAllByTournamentRoundId(@Param("tournamentRoundId") Long tournamentRoundId);
}
