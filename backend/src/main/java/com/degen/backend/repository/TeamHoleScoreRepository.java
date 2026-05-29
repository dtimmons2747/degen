package com.degen.backend.repository;

import com.degen.backend.entity.TeamHoleScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamHoleScoreRepository extends JpaRepository<TeamHoleScore, Long> {
    
    @Query("SELECT ths FROM TeamHoleScore ths WHERE ths.roundTeam.id = :roundTeamId ORDER BY ths.hole.holeNumber")
    List<TeamHoleScore> findByRoundTeamId(@Param("roundTeamId") Long roundTeamId);

    @Query("SELECT ths FROM TeamHoleScore ths WHERE ths.roundTeam.id = :roundTeamId AND ths.hole.id = :holeId")
    Optional<TeamHoleScore> findByRoundTeamIdAndHoleId(@Param("roundTeamId") Long roundTeamId, @Param("holeId") Long holeId);

    @Query("SELECT ths FROM TeamHoleScore ths WHERE ths.roundTeam.roundTeeTime.tournamentRound.id = :tournamentRoundId AND ths.hole.id = :holeId")
    List<TeamHoleScore> findByTournamentRoundIdAndHoleId(@Param("tournamentRoundId") Long tournamentRoundId, @Param("holeId") Long holeId);

    @Query("SELECT ths FROM TeamHoleScore ths WHERE ths.roundTeam.roundTeeTime.tournamentRound.id = :tournamentRoundId")
    List<TeamHoleScore> findByTournamentRoundId(@Param("tournamentRoundId") Long tournamentRoundId);
}
