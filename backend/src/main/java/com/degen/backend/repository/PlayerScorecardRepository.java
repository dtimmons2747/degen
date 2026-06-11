package com.degen.backend.repository;

import com.degen.backend.entity.PlayerScorecard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerScorecardRepository extends JpaRepository<PlayerScorecard, Long> {
    
    @Query("SELECT ps FROM PlayerScorecard ps " +
           "JOIN FETCH ps.roundTeeTime " +
           "LEFT JOIN FETCH ps.player " +
           "LEFT JOIN FETCH ps.hole " +
           "WHERE ps.roundTeeTime.id = :roundTeeTimeId " +
           "ORDER BY ps.hole.holeNumber")
    List<PlayerScorecard> findByRoundTeeTimeId(@Param("roundTeeTimeId") Long roundTeeTimeId);
    
    @Query("SELECT ps FROM PlayerScorecard ps " +
           "JOIN FETCH ps.roundTeeTime " +
           "LEFT JOIN FETCH ps.player " +
           "LEFT JOIN FETCH ps.hole " +
           "WHERE ps.roundTeeTime.id = :roundTeeTimeId AND ps.player.id = :playerId " +
           "ORDER BY ps.hole.holeNumber")
    List<PlayerScorecard> findByRoundTeeTimeIdAndPlayerId(@Param("roundTeeTimeId") Long roundTeeTimeId, @Param("playerId") Long playerId);
    
    @Query("SELECT ps FROM PlayerScorecard ps " +
           "JOIN FETCH ps.roundTeeTime " +
           "LEFT JOIN FETCH ps.player " +
           "LEFT JOIN FETCH ps.hole " +
           "WHERE ps.roundTeeTime.id = :roundTeeTimeId AND ps.player.id = :playerId AND ps.hole.id = :holeId " +
           "ORDER BY ps.id DESC")
    List<PlayerScorecard> findByRoundTeeTimeIdAndPlayerIdAndHoleId(
        @Param("roundTeeTimeId") Long roundTeeTimeId,
        @Param("playerId") Long playerId,
        @Param("holeId") Long holeId
    );
    
    @Query("SELECT COUNT(ps) FROM PlayerScorecard ps WHERE ps.roundTeeTime.id = :roundTeeTimeId")
    long countByRoundTeeTimeId(@Param("roundTeeTimeId") Long roundTeeTimeId);
    
    @Query("SELECT ps FROM PlayerScorecard ps " +
           "JOIN FETCH ps.roundTeeTime rtt " +
           "JOIN FETCH rtt.tournamentRound tr " +
           "JOIN FETCH tr.course c " +
           "LEFT JOIN FETCH ps.hole h " +
           "WHERE ps.player.id = :playerId " +
           "ORDER BY tr.day DESC, rtt.id DESC")
    List<PlayerScorecard> findPlayerScorecardsByPlayerIdOrderByDate(@Param("playerId") Long playerId);
}
