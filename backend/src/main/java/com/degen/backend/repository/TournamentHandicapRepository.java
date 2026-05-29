package com.degen.backend.repository;

import com.degen.backend.entity.Player;
import com.degen.backend.entity.TournamentHandicap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentHandicapRepository extends JpaRepository<TournamentHandicap, Long> {

    @Query("SELECT th.player FROM TournamentHandicap th WHERE th.tournament.id = :tournamentId ORDER BY th.tournament.year DESC")
    List<Player> findPlayersByTournamentId(@Param("tournamentId") Long tournamentId);

    @Query("SELECT th FROM TournamentHandicap th WHERE th.tournament.id = :tournamentId ORDER BY th.tournament.year DESC")
    List<TournamentHandicap> findByTournamentId(@Param("tournamentId") Long tournamentId);

    @Query("SELECT th FROM TournamentHandicap th WHERE th.tournament.id = :tournamentId AND th.player.id = :playerId ORDER BY th.tournament.year DESC")
    List<TournamentHandicap> findByTournamentIdAndPlayerId(@Param("tournamentId") Long tournamentId,
            @Param("playerId") Long playerId);

    @Query("SELECT th FROM TournamentHandicap th WHERE th.player.id = :playerId AND th.tournament.id = :tournamentId")
    Optional<TournamentHandicap> findByPlayerIdAndTournamentId(@Param("playerId") Long playerId,
            @Param("tournamentId") Long tournamentId);
}