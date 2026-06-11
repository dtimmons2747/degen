package com.degen.backend.service;

import com.degen.backend.entity.PlayerScorecard;
import com.degen.backend.entity.TournamentHandicap;
import com.degen.backend.entity.Hole;
import com.degen.backend.entity.TeamHoleScore;
import com.degen.backend.entity.RoundTeam;
import com.degen.backend.entity.RoundTeeTime;
import com.degen.backend.entity.RoundHandicap;
import com.degen.backend.repository.PlayerScorecardRepository;
import com.degen.backend.repository.TournamentHandicapRepository;
import com.degen.backend.repository.HoleRepository;
import com.degen.backend.repository.TeamHoleScoreRepository;
import com.degen.backend.repository.RoundTeamRepository;
import com.degen.backend.repository.RoundTeeTimeRepository;
import com.degen.backend.repository.RoundHandicapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlayerScorecardService {

    @Autowired
    private PlayerScorecardRepository playerScorecardRepository;

    @Autowired
    private TournamentHandicapRepository tournamentHandicapRepository;

    @Autowired
    private HoleRepository holeRepository;

    @Autowired
    private TeamHoleScoreRepository teamHoleScoreRepository;

    @Autowired
    private RoundTeamRepository roundTeamRepository;

    @Autowired
    private RoundTeeTimeRepository roundTeeTimeRepository;

    @Autowired
    private RoundHandicapRepository roundHandicapRepository;

    public List<PlayerScorecard> getAllScorecards() {
        return playerScorecardRepository.findAll();
    }

    public Optional<PlayerScorecard> getScorecardById(Long id) {
        return playerScorecardRepository.findById(id);
    }

    public List<PlayerScorecard> getScorecardsByRoundTeeTimeId(Long roundTeeTimeId) {
        return playerScorecardRepository.findByRoundTeeTimeId(roundTeeTimeId);
    }

    public List<PlayerScorecard> getScorecardsByRoundTeeTimeIdAndPlayerId(Long roundTeeTimeId, Long playerId) {
        return playerScorecardRepository.findByRoundTeeTimeIdAndPlayerId(roundTeeTimeId, playerId);
    }

    public Optional<PlayerScorecard> getScorecardByRoundTeeTimeIdAndPlayerIdAndHoleId(Long roundTeeTimeId,
            Long playerId, Long holeId) {
        List<PlayerScorecard> scorecards = playerScorecardRepository
                .findByRoundTeeTimeIdAndPlayerIdAndHoleId(roundTeeTimeId, playerId, holeId);
        return scorecards.isEmpty() ? Optional.empty() : Optional.of(scorecards.get(0));
    }

    public long countScorecardsByRoundTeeTimeId(Long roundTeeTimeId) {
        return playerScorecardRepository.countByRoundTeeTimeId(roundTeeTimeId);
    }

    public PlayerScorecard saveScorecard(PlayerScorecard scorecard) {
        try {
            System.out.println("saveScorecard called - playerId="
                    + (scorecard.getPlayer() != null ? scorecard.getPlayer().getId() : "NULL") +
                    ", holeId=" + (scorecard.getHole() != null ? scorecard.getHole().getId() : "NULL") +
                    ", grossScore=" + scorecard.getGrossScore());

            // Calculate net score if gross score is provided
            if (scorecard.getGrossScore() != null && scorecard.getHole() != null) {
                scorecard.setNetScore(calculateNetScore(scorecard));
                System.out.println("  netScore calculated: " + scorecard.getNetScore());
            }
            PlayerScorecard saved = playerScorecardRepository.save(scorecard);
            System.out.println("  PlayerScorecard saved with id=" + saved.getId());

            // Create/update TeamHoleScore entry for stroke-based scoring
            createOrUpdateTeamHoleScore(saved);

            return saved;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error saving scorecard: " + e.getMessage(), e);
        }
    }

    private void createOrUpdateTeamHoleScore(PlayerScorecard scorecard) {
        try {
            System.out.println("createOrUpdateTeamHoleScore called");

            if (scorecard.getRoundTeeTime() == null) {
                System.out.println("  SKIPPED: roundTeeTime is null");
                return;
            }
            if (scorecard.getPlayer() == null) {
                System.out.println("  SKIPPED: player is null");
                return;
            }
            if (scorecard.getHole() == null) {
                System.out.println("  SKIPPED: hole is null");
                return;
            }
            if (scorecard.getGrossScore() == null) {
                System.out.println("  SKIPPED: grossScore is null");
                return;
            }

            Long teeTimeId = scorecard.getRoundTeeTime().getId();
            Long playerId = scorecard.getPlayer().getId();
            Long holeId = scorecard.getHole().getId();

            System.out.println("  Looking for team for player " + playerId + " in teeTime " + teeTimeId);

            // Get full RoundTeeTime object (frontend only sends ID)
            Optional<RoundTeeTime> rttOpt = roundTeeTimeRepository.findById(teeTimeId);
            if (!rttOpt.isPresent()) {
                System.out.println("  WARNING: RoundTeeTime " + teeTimeId + " not found!");
                return;
            }
            RoundTeeTime roundTeeTime = rttOpt.get();

            // Find which team this player is on
            List<RoundTeam> teams = roundTeamRepository.findByRoundTeeTimeId(teeTimeId);
            System.out.println("  Found " + teams.size() + " teams in teeTime");

            RoundTeam playerTeam = null;

            for (RoundTeam team : teams) {
                System.out.println("    Team " + team.getId() + ": p1=" + team.getPlayer1Id() + ", p2="
                        + team.getPlayer2Id() + ", p3=" + team.getPlayer3Id());
                if ((team.getPlayer1Id() != null && team.getPlayer1Id().equals(playerId)) ||
                        (team.getPlayer2Id() != null && team.getPlayer2Id().equals(playerId)) ||
                        (team.getPlayer3Id() != null && team.getPlayer3Id().equals(playerId))) {
                    playerTeam = team;
                    System.out.println("    -> Player " + playerId + " is on this team");
                    break;
                }
            }

            if (playerTeam == null) {
                System.out.println("  WARNING: Player " + playerId + " not found on any team!");
                return;
            }

            // Get full Hole object
            Optional<Hole> holeOpt = holeRepository.findById(holeId);
            if (!holeOpt.isPresent()) {
                System.out.println("  WARNING: Hole " + holeId + " not found!");
                return;
            }
            Hole hole = holeOpt.get();

            // Get TournamentRound from the fetched RoundTeeTime
            if (roundTeeTime.getTournamentRound() == null) {
                System.out.println("  WARNING: TournamentRound is null on RoundTeeTime " + teeTimeId);
                return;
            }

            // Calculate team net score based on game type (aggregate vs best ball)
            Long gameId = roundTeeTime.getTournamentRound().getGame() != null
                    ? roundTeeTime.getTournamentRound().getGame().getId()
                    : null;
            Long scoringTypeId = roundTeeTime.getTournamentRound().getScoringType() != null
                    ? roundTeeTime.getTournamentRound().getScoringType().getId()
                    : null;
            System.out.println("  Game ID: " + gameId + ", Scoring Type ID: " + scoringTypeId);
            Integer teamNetScore = calculateTeamNetScore(playerTeam, holeId, gameId, scoringTypeId);
            System.out.println("  Calculated team net score: " + teamNetScore);

            // Find or create TeamHoleScore
            Optional<TeamHoleScore> existing = teamHoleScoreRepository.findByRoundTeamIdAndHoleId(playerTeam.getId(),
                    holeId);

            TeamHoleScore teamHoleScore;
            if (existing.isPresent()) {
                teamHoleScore = existing.get();
                System.out.println("  Updating TeamHoleScore (id=" + teamHoleScore.getId() + ") for team "
                        + playerTeam.getId() + ", hole " + holeId + " with netScore " + teamNetScore);
            } else {
                teamHoleScore = new TeamHoleScore();
                teamHoleScore.setRoundTeam(playerTeam);
                teamHoleScore.setHole(hole);
                teamHoleScore.setTournamentRound(roundTeeTime.getTournamentRound());
                System.out.println("  Creating new TeamHoleScore for team " + playerTeam.getId() + ", hole " + holeId
                        + " with netScore " + teamNetScore);
            }

            // Set team net score (calculated based on scoring type)
            if (teamNetScore != null) {
                teamHoleScore.setNetScore(teamNetScore);
            }
            // gamePoints remains null for now (will be set when calculated by button)

            teamHoleScoreRepository.save(teamHoleScore);
            System.out.println("  TeamHoleScore saved successfully");
        } catch (Exception e) {
            System.err.println("Error updating TeamHoleScore: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Calculate team net score for a hole based on game type.
     * gameId determines HOW to calculate team score:
     * - 2-Man Best Ball (gameId 1): Best (lowest) net score from team members
     * - Nines (gameId 3): Best (lowest) net score from team members
     * - Other games (2-Man Aggregate, etc): Sum of all team members' net scores
     * 
     * Note: scoringTypeId is not needed for team score calculation; it determines
     * how that team score is USED in other parts of the system (e.g., game points).
     */
    private Integer calculateTeamNetScore(RoundTeam team, Long holeId, Long gameId, Long scoringTypeId) {
        List<Long> teamPlayerIds = new java.util.ArrayList<>();
        if (team.getPlayer1Id() != null)
            teamPlayerIds.add(team.getPlayer1Id());
        if (team.getPlayer2Id() != null)
            teamPlayerIds.add(team.getPlayer2Id());
        if (team.getPlayer3Id() != null)
            teamPlayerIds.add(team.getPlayer3Id());

        // Get all team members' net scores for this hole
        List<Integer> netScores = new java.util.ArrayList<>();
        for (Long playerId : teamPlayerIds) {
            List<PlayerScorecard> scorecards = playerScorecardRepository.findByRoundTeeTimeIdAndPlayerIdAndHoleId(
                    team.getRoundTeeTime().getId(), playerId, holeId);
            if (!scorecards.isEmpty() && scorecards.get(0).getNetScore() != null) {
                netScores.add(scorecards.get(0).getNetScore());
                System.out.println("    Player " + playerId + " netScore: " + scorecards.get(0).getNetScore());
            }
        }

        if (netScores.isEmpty()) {
            System.out.println("    No net scores found for team");
            return null;
        }

        // 2-Man Best Ball (gameId 1) or Nines (gameId 3): take best (lowest) net score
        if (gameId != null && (gameId == 1L || gameId == 3L)) {
            Integer bestNet = netScores.stream().min(Integer::compareTo).orElse(null);
            System.out.println("    Best Ball format (gameId " + gameId + "): best net score = " + bestNet);
            return bestNet;
        }

        // Other games (2-Man Aggregate, etc): sum of all net scores
        Integer sumNet = netScores.stream().reduce(0, Integer::sum);
        System.out.println("    Aggregate format (gameId " + gameId + "): sum of net scores = " + sumNet);
        return sumNet;
    }

    public void deleteScorecard(Long id) {
        try {
            playerScorecardRepository.deleteById(id);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error deleting scorecard: " + e.getMessage(), e);
        }
    }

    public boolean hasScorecards(Long roundTeeTimeId) {
        return playerScorecardRepository.countByRoundTeeTimeId(roundTeeTimeId) > 0;
    }

    public Integer calculateTotal(Long roundTeeTimeId, Long playerId) {
        List<PlayerScorecard> scorecards = getScorecardsByRoundTeeTimeIdAndPlayerId(roundTeeTimeId, playerId);
        int total = 0;
        for (PlayerScorecard scorecard : scorecards) {
            if (scorecard.getGrossScore() != null) {
                total += scorecard.getGrossScore();
            }
        }
        return total;
    }

    private Integer calculateNetScore(PlayerScorecard scorecard) {
        if (scorecard.getGrossScore() == null || scorecard.getHole() == null) {
            return null;
        }

        // Get course handicap from the request body if provided, otherwise look it up
        Integer courseHandicap = scorecard.getCourseHandicap();

        if (courseHandicap == null && scorecard.getRoundTeeTime() != null && scorecard.getPlayer() != null) {
            RoundTeeTime roundTeeTime = scorecard.getRoundTeeTime();
            Long playerId = scorecard.getPlayer().getId();
            Long roundTeeTimeId = roundTeeTime.getId();

            // PRIORITY 1: Check round_handicap table for per-round override
            Optional<RoundHandicap> roundHandicapOpt = roundHandicapRepository
                    .findByRoundTeeTimeIdAndPlayerId(roundTeeTimeId, playerId);

            if (roundHandicapOpt.isPresent() && roundHandicapOpt.get().getHandicap() != null) {
                courseHandicap = roundHandicapOpt.get().getHandicap().intValue();
            } else {
                // PRIORITY 2: Fall back to tournament handicap
                Long tournamentId = roundTeeTime.getTournamentRound().getTournament().getId();
                List<TournamentHandicap> handicaps = tournamentHandicapRepository
                        .findByTournamentIdAndPlayerId(tournamentId, playerId);
                if (!handicaps.isEmpty()) {
                    Double playerHandicap = handicaps.get(0).getHandicap();
                    courseHandicap = playerHandicap != null ? playerHandicap.intValue() : null;
                } else {
                    // No handicap found, net score = gross score
                    return scorecard.getGrossScore();
                }
            }
        }

        if (courseHandicap == null) {
            // No handicap available, net score = gross score
            return scorecard.getGrossScore();
        }

        // Fetch complete Hole object from database (frontend only sends hole ID)
        Long holeId = scorecard.getHole().getId();
        Optional<Hole> holeOpt = holeRepository.findById(holeId);

        if (!holeOpt.isPresent() || holeOpt.get().getHandicap() == null) {
            // No hole data or no handicap, return gross score
            return scorecard.getGrossScore();
        }

        Hole hole = holeOpt.get();
        Integer grossScore = scorecard.getGrossScore();
        Integer holeHandicap = hole.getHandicap();

        // Calculate strokes to subtract based on course handicap
        // If courseHandicap is 18, subtract 1 from each hole
        // If courseHandicap is 36, subtract 2 from each hole
        // If courseHandicap is 11, subtract 1 from the 11 hardest holes (highest
        // handicap)

        int baseSubtraction = courseHandicap / 18;
        int remainingSubtraction = courseHandicap % 18;

        // Additional stroke if this hole is among the remaining hardest holes
        int additionalStroke = (holeHandicap <= remainingSubtraction) ? 1 : 0;

        int totalStrokesToSubtract = baseSubtraction + additionalStroke;
        int netScore = grossScore - totalStrokesToSubtract;

        return Math.max(0, netScore);
    }

    public void recalculateScoresForTeeTime(Long roundTeeTimeId, java.util.Set<Long> affectedPlayerIds) {
        try {
            // Get scorecards only for the affected players
            List<PlayerScorecard> scorecards = playerScorecardRepository.findByRoundTeeTimeId(roundTeeTimeId);

            for (PlayerScorecard scorecard : scorecards) {
                // Only update scorecards for players whose handicaps changed
                if (scorecard.getPlayer() != null && affectedPlayerIds.contains(scorecard.getPlayer().getId()) &&
                        scorecard.getGrossScore() != null) {

                    // Recalculate net score with the new handicap
                    Integer newNetScore = calculateNetScore(scorecard);
                    scorecard.setNetScore(newNetScore);

                    // Save the updated scorecard
                    playerScorecardRepository.save(scorecard);

                    // Update TeamHoleScore if applicable
                    createOrUpdateTeamHoleScore(scorecard);
                }
            }
        } catch (Exception e) {
            System.err.println("Error recalculating scores for tee time: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
