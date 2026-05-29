package com.degen.backend.repository;

import com.degen.backend.entity.TournamentRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentRoundRepository extends JpaRepository<TournamentRound, Long> {
    
    @Query("SELECT tr FROM TournamentRound tr WHERE tr.tournament.id = :tournamentId ORDER BY tr.tournament.year DESC")
    List<TournamentRound> findByTournamentId(@Param("tournamentId") Long tournamentId);
}
