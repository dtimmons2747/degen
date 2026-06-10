package com.degen.backend.service;

import com.degen.backend.entity.*;
import com.degen.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeamGamePointsService {

    @Autowired
    private RoundTeamRepository roundTeamRepository;

    @Autowired
    private TeamHoleScoreRepository teamHoleScoreRepository;

    @Autowired
    private PlayerScorecardRepository playerScorecardRepository;

    @Autowired
    private HoleRepository holeRepository;

    @Autowired
    private RoundTeeTimeRepository roundTeeTimeRepository;

    /**
     * Calculate and save game points for all teams in a tournament round
     * Compares team best net scores across all tee times in the round
     */
    @Transactional
    public void calculateAndSaveTeamGamePoints(Long tournamentRoundId) {
        try {
            // Get the tournament round to access the course and game
            TournamentRound tournamentRound = null;
            List<RoundTeeTime> roundTeeTimes = roundTeeTimeRepository.findAllByTournamentRoundId(tournamentRoundId);
            
            if (roundTeeTimes.isEmpty()) {
                throw new IllegalStateException("No tee times found for round " + tournamentRoundId);
            }
            
            // Get round from first tee time
            tournamentRound = roundTeeTimes.get(0).getTournamentRound();
            if (tournamentRound == null || tournamentRound.getCourse() == null) {
                throw new IllegalStateException("No course found for round " + tournamentRoundId);
            }
            
            // Check if this is an Individual game
            if (tournamentRound.getGame() != null && tournamentRound.getGame().getId() == 4L) {
                // Individual game - handle separately
                calculateAndSaveIndividualGamePoints(tournamentRoundId);
                return;
            }
            
            // Team-based game logic (Nines, 2-Man Aggregate, etc.)
            // Get holes for this round's course
            List<Hole> holes = holeRepository.findByCourseId(tournamentRound.getCourse().getId());
            
            if (holes.isEmpty()) {
                throw new IllegalStateException("No holes found for course " + tournamentRound.getCourse().getId());
            }
            
            // Generate team hole scores for each tee time
            for (RoundTeeTime teeTime : roundTeeTimes) {
                generateTeamHoleScores(teeTime.getId());
            }

            // Collect all teams from all tee times
            List<RoundTeam> allTeamsInRound = new java.util.ArrayList<>();
            for (RoundTeeTime teeTime : roundTeeTimes) {
                List<RoundTeam> teamsForTeeTime = roundTeamRepository.findByRoundTeeTimeId(teeTime.getId());
                allTeamsInRound.addAll(teamsForTeeTime);
            }
            
            if (allTeamsInRound.isEmpty()) {
                throw new IllegalStateException("No teams found for round " + tournamentRoundId);
            }
            
            // Validate that all teams have complete scorecards for all holes
            validateAllTeamsHaveCompleteScorecardsForAllHolesRoundLevel(allTeamsInRound, holes);

            // For each hole, calculate team best scores and assign points
            for (Hole hole : holes) {
                calculateTeamPointsForHole(tournamentRoundId, hole);
            }

            // Calculate player game points for Nines (id 4) and Stableford (id 3)
            // 2-Man Aggregate (Stroke, id 1) is team-only competition
            Long scoringTypeId = (tournamentRound.getScoringType() != null)
                ? tournamentRound.getScoringType().getId()
                : null;
            
            if (scoringTypeId != null && (scoringTypeId == 4L || scoringTypeId == 3L)) {
                // For each hole, calculate player points within each team (Nines and Stableford)
                for (Hole hole : holes) {
                    if (scoringTypeId == 4L) {
                        calculatePlayerPointsForHole(allTeamsInRound, hole);
                    } else if (scoringTypeId == 3L) {
                        calculatePlayerStablefordPointsForHole(allTeamsInRound, hole);
                    }
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error calculating team game points: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate and save game points for an Individual game round
     * For Split Skins (scoring type id = 5): Find best net score on each hole,
     * divide 1 by the number of players with that score
     * For Stroke (scoring type id = 1): No game points calculated
     */
    @Transactional
    private void calculateAndSaveIndividualGamePoints(Long tournamentRoundId) {
        try {
            // Get the tournament round
            List<RoundTeeTime> roundTeeTimes = roundTeeTimeRepository.findAllByTournamentRoundId(tournamentRoundId);
            if (roundTeeTimes.isEmpty()) {
                throw new IllegalStateException("No tee times found for round " + tournamentRoundId);
            }

            TournamentRound tournamentRound = roundTeeTimes.get(0).getTournamentRound();
            if (tournamentRound == null || tournamentRound.getCourse() == null) {
                throw new IllegalStateException("No course found for round " + tournamentRoundId);
            }

            Long scoringTypeId = (tournamentRound.getScoringType() != null)
                ? tournamentRound.getScoringType().getId()
                : null;

            // If Stroke (id=1), no game points needed for Individual game
            if (scoringTypeId == null || scoringTypeId == 1L) {
                return; // No game points for Stroke Individual
            }

            // Get holes for this round's course
            List<Hole> holes = holeRepository.findByCourseId(tournamentRound.getCourse().getId());
            if (holes.isEmpty()) {
                throw new IllegalStateException("No holes found for course " + tournamentRound.getCourse().getId());
            }

            // Get all players' scorecards for this round and validate completion
            List<PlayerScorecard> allPlayerScorecards = new java.util.ArrayList<>();
            Map<Long, String> playerNames = new java.util.HashMap<>();
            Set<Long> playerIds = new java.util.HashSet<>();

            for (RoundTeeTime teeTime : roundTeeTimes) {
                List<PlayerScorecard> teeTimeScorecards = playerScorecardRepository.findByRoundTeeTimeId(teeTime.getId());
                for (PlayerScorecard sc : teeTimeScorecards) {
                    allPlayerScorecards.add(sc);
                    Long pId = sc.getPlayer().getId();
                    playerIds.add(pId);
                    playerNames.put(pId, sc.getPlayer().getFirstName() + " " + sc.getPlayer().getLastName());
                }
            }

            if (allPlayerScorecards.isEmpty()) {
                throw new IllegalStateException("No player scorecards found for round " + tournamentRoundId);
            }

            // Validate all players have complete scorecards for all holes
            validateAllPlayersHaveCompleteScorecardsForAllHolesIndividual(playerIds, allPlayerScorecards, holes);

            // If Split Skins (id=5), calculate game points
            if (scoringTypeId == 5L) {
                calculateSplitSkinsGamePoints(tournamentRoundId, roundTeeTimes, tournamentRound);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error calculating individual game points: " + e.getMessage(), e);
        }
    }

    /**
     * Validate that all players have complete scorecards for all holes
     */
    private void validateAllPlayersHaveCompleteScorecardsForAllHolesIndividual(Set<Long> playerIds, 
                                                                               List<PlayerScorecard> allScorecards,
                                                                               List<Hole> holes) {
        StringBuilder missingScores = new StringBuilder();

        for (Long playerId : playerIds) {
            for (Hole hole : holes) {
                boolean hasScore = allScorecards.stream()
                    .filter(sc -> sc.getPlayer().getId().equals(playerId))
                    .filter(sc -> sc.getHole().getId().equals(hole.getId()))
                    .anyMatch(sc -> sc.getGrossScore() != null);

                if (!hasScore) {
                    missingScores.append("Player ")
                        .append(playerId)
                        .append(" is missing score for hole ")
                        .append(hole.getHoleNumber())
                        .append(". ");
                }
            }
        }

        if (missingScores.length() > 0) {
            throw new IllegalStateException("Cannot calculate game points. " + missingScores.toString() +
                    "All " + playerIds.size() + " players must have complete scores for all holes before game points can be calculated.");
        }
    }

    /**
     * Calculate Split Skins game points for Individual game
     * For each hole, find all players with best (lowest) net score,
     * and divide 1 by that count. Each gets 1/count points (rounded to 2 decimals)
     */
    private void calculateSplitSkinsGamePoints(Long tournamentRoundId, List<RoundTeeTime> roundTeeTimes, TournamentRound tournamentRound) {
        // Get holes for this course
        List<Hole> holes = holeRepository.findByCourseId(tournamentRound.getCourse().getId());
        if (holes.isEmpty()) {
            throw new IllegalStateException("No holes found for course " + tournamentRound.getCourse().getId());
        }

        // Get all players' scorecards for this round
        List<PlayerScorecard> allPlayerScorecards = new java.util.ArrayList<>();
        for (RoundTeeTime teeTime : roundTeeTimes) {
            List<PlayerScorecard> teeTimeScorecards = playerScorecardRepository.findByRoundTeeTimeId(teeTime.getId());
            allPlayerScorecards.addAll(teeTimeScorecards);
        }

        if (allPlayerScorecards.isEmpty()) {
            throw new IllegalStateException("No player scorecards found for round " + tournamentRoundId);
        }

        // For each hole, calculate split skins game points
        for (Hole hole : holes) {
            // Get all player scorecards for this hole
            List<PlayerScorecard> scoresForHole = allPlayerScorecards.stream()
                .filter(ps -> ps.getHole() != null && ps.getHole().getId().equals(hole.getId()))
                .filter(ps -> ps.getNetScore() != null)
                .collect(Collectors.toList());

            if (scoresForHole.isEmpty()) {
                continue; // No scores for this hole
            }

            // Find best (lowest) net score
            Integer bestScore = scoresForHole.stream()
                .map(PlayerScorecard::getNetScore)
                .min(Integer::compareTo)
                .orElse(null);

            if (bestScore == null) {
                continue;
            }

            // Count how many players have the best score
            List<PlayerScorecard> winnersForHole = scoresForHole.stream()
                .filter(ps -> ps.getNetScore().equals(bestScore))
                .collect(Collectors.toList());

            int winnerCount = winnersForHole.size();
            double pointsPerWinner = Math.round(100.0 / winnerCount) / 100.0; // 1/count, rounded to 2 decimals

            // Assign game points to all players for this hole
            for (PlayerScorecard scorecard : scoresForHole) {
                if (winnersForHole.contains(scorecard)) {
                    scorecard.setGamePoints((int) Math.round(pointsPerWinner * 100)); // Store as cents for now
                } else {
                    scorecard.setGamePoints(0); // No points for non-winners
                }
                playerScorecardRepository.save(scorecard);
            }
        }
    }

    private void calculateTeamPointsForHole(Long tournamentRoundId, Hole hole) {
        try {
            // Get the tournament round to determine scoring type
            List<RoundTeeTime> roundTeeTimes = roundTeeTimeRepository.findAllByTournamentRoundId(tournamentRoundId);
            if (roundTeeTimes.isEmpty()) {
                return;
            }
            TournamentRound tournamentRound = roundTeeTimes.get(0).getTournamentRound();
            Long scoringTypeId = (tournamentRound.getScoringType() != null)
                ? tournamentRound.getScoringType().getId()
                : null;

            // Get all team hole scores for this hole in this round
            List<TeamHoleScore> teamScoresForHole = teamHoleScoreRepository
                .findByTournamentRoundIdAndHoleId(tournamentRoundId, hole.getId());

            if (teamScoresForHole.isEmpty()) {
                return;
            }

            // Sort by net score
            List<TeamHoleScore> sortedByScore = teamScoresForHole.stream()
                .sorted(Comparator.comparingInt(ths -> ths.getNetScore() != null ? ths.getNetScore() : Integer.MAX_VALUE))
                .collect(Collectors.toList());

            // Calculate points based on scoring type
            List<Integer> gamePoints;
            if (scoringTypeId != null && scoringTypeId == 3L) {
                // Stableford scoring
                gamePoints = calculateStablefordGamePoints(sortedByScore, hole);
            } else {
                // Nines scoring (default for team games with points)
                gamePoints = calculateGamePoints(sortedByScore);
            }

            // Save game points for each team
            for (int i = 0; i < sortedByScore.size(); i++) {
                TeamHoleScore teamScore = sortedByScore.get(i);
                teamScore.setGamePoints(gamePoints.get(i));
                teamHoleScoreRepository.save(teamScore);
            }
        } catch (Exception e) {
            // Log error but don't throw - continue processing other holes
        }
    }

    /**
     * Calculate player game points for a specific hole within each team
     * Each player competes against their teammates on this hole
     * For 3-player teams (Nines): 5/3/1 points for best/2nd/3rd player
     */
    private void calculatePlayerPointsForHole(List<RoundTeam> allTeams, Hole hole) {
        try {
            for (RoundTeam team : allTeams) {
                // Get all players on this team
                List<Long> teamPlayerIds = new java.util.ArrayList<>();
                if (team.getPlayer1Id() != null && team.getPlayer1Id() > 0) {
                    teamPlayerIds.add(team.getPlayer1Id());
                }
                if (team.getPlayer2Id() != null && team.getPlayer2Id() > 0) {
                    teamPlayerIds.add(team.getPlayer2Id());
                }
                if (team.getPlayer3Id() != null && team.getPlayer3Id() > 0) {
                    teamPlayerIds.add(team.getPlayer3Id());
                }

                if (teamPlayerIds.isEmpty()) {
                    continue; // No players on this team
                }

                // Get scorecards for all team members on this hole
                List<PlayerScorecard> teamScorecardsForHole = new java.util.ArrayList<>();

                for (Long playerId : teamPlayerIds) {
                    Optional<PlayerScorecard> scorecard = playerScorecardRepository
                        .findByRoundTeeTimeIdAndPlayerIdAndHoleId(team.getRoundTeeTime().getId(), playerId, hole.getId());
                    
                    if (scorecard.isPresent() && scorecard.get().getNetScore() != null) {
                        teamScorecardsForHole.add(scorecard.get());
                    }
                }

                // Need at least 1 player scorecard to assign points
                if (teamScorecardsForHole.isEmpty()) {
                    continue;
                }

                // Sort players by net score (ascending = best first)
                List<PlayerScorecard> sortedByScore = teamScorecardsForHole.stream()
                    .sorted(Comparator.comparingInt(ps -> ps.getNetScore() != null ? ps.getNetScore() : Integer.MAX_VALUE))
                    .collect(Collectors.toList());

                // Calculate player points within this team (3 players = 5/3/1)
                List<Integer> playerPoints = calculatePlayerGamePoints(sortedByScore);

                // Save points for each player
                for (int i = 0; i < sortedByScore.size(); i++) {
                    PlayerScorecard scorecard = sortedByScore.get(i);
                    scorecard.setGamePoints(playerPoints.get(i));
                    playerScorecardRepository.save(scorecard);
                }
            }
        } catch (Exception e) {
            // Log error but don't throw - continue processing other teams
        }
    }

    /**
     * Calculate player game points based on net scores (within a team on a single hole)
     * Uses same 5/3/1 scoring as team competition
     * Handles ties by splitting points evenly
     */
    private List<Integer> calculatePlayerGamePoints(List<PlayerScorecard> sortedByScore) {
        List<Integer> points = new ArrayList<>();

        for (PlayerScorecard score : sortedByScore) {
            points.add(0); // Placeholder
        }

        int numPlayers = sortedByScore.size();

        // Only support 3-player scoring for now (Nines teams)
        if (numPlayers != 3) {
            // Equal distribution if not 3 players
            int pointsPerPlayer = 9 / numPlayers;
            for (int i = 0; i < numPlayers; i++) {
                points.set(i, pointsPerPlayer);
            }
            return points;
        }

        // 3-player Nines scoring (same as team scoring: 5/3/1)
        Integer score1 = sortedByScore.get(0).getNetScore();
        Integer score2 = sortedByScore.get(1).getNetScore();
        Integer score3 = sortedByScore.get(2).getNetScore();

        // All tied
        if (score1.equals(score2) && score2.equals(score3)) {
            return Arrays.asList(3, 3, 3);
        }

        // Tie for first place (1st and 2nd tied, 3rd different)
        if (score1.equals(score2) && !score2.equals(score3)) {
            return Arrays.asList(4, 4, 1);
        }

        // Tie for second place (1st different, 2nd and 3rd tied)
        if (!score1.equals(score2) && score2.equals(score3)) {
            return Arrays.asList(5, 2, 2);
        }

        // Standard: no ties (5/3/1)
        return Arrays.asList(5, 3, 1);
    }

    /**
     * Get or create team hole scores for all players in a tee time
     * Calculates best net score for the group
     */
    @Transactional
    public void generateTeamHoleScores(Long roundTeeTimeId) {
        try {
            // Get the tee time and its team reference
            Optional<RoundTeeTime> teeTimeOpt = roundTeeTimeRepository.findById(roundTeeTimeId);
            if (!teeTimeOpt.isPresent()) {
                return;
            }

            RoundTeeTime teeTime = teeTimeOpt.get();
            List<RoundTeam> teams = roundTeamRepository.findByRoundTeeTimeId(roundTeeTimeId);

            if (teams.isEmpty()) {
                return;
            }

            // Get course from tee time's tournament round
            if (teeTime.getTournamentRound() == null || teeTime.getTournamentRound().getCourse() == null) {
                return;
            }

            // Get holes for this course
            List<Hole> holes = holeRepository.findByCourseId(teeTime.getTournamentRound().getCourse().getId());

            // For each hole, find best net score for all players in this tee time
            for (Hole hole : holes) {
                for (RoundTeam team : teams) {
                    generateTeamHoleScore(team, hole, teeTime);
                }
            }
        } catch (Exception e) {
            // Log error but don't throw - continue processing
        }
    }

    private void generateTeamHoleScore(RoundTeam team, Hole hole, RoundTeeTime teeTime) {
        try {
            // Get scorecards for this team's players on this hole
            List<Long> teamPlayerIds = new java.util.ArrayList<>();
            if (team.getPlayer1Id() != null && team.getPlayer1Id() > 0) {
                teamPlayerIds.add(team.getPlayer1Id());
            }
            if (team.getPlayer2Id() != null && team.getPlayer2Id() > 0) {
                teamPlayerIds.add(team.getPlayer2Id());
            }
            if (team.getPlayer3Id() != null && team.getPlayer3Id() > 0) {
                teamPlayerIds.add(team.getPlayer3Id());
            }

            if (teamPlayerIds.isEmpty()) {
                return; // No players in this team
            }

            // Only load scorecards for this specific tee time (not all scorecards)
            List<PlayerScorecard> teamScorecards = playerScorecardRepository.findByRoundTeeTimeId(teeTime.getId()).stream()
                .filter(sc -> teamPlayerIds.contains(sc.getPlayer().getId()))
                .filter(sc -> sc.getHole().getId().equals(hole.getId()))
                .collect(Collectors.toList());

            // Find best net score for this team on this hole
            Integer bestNetScore = teamScorecards.stream()
                .map(PlayerScorecard::getNetScore)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(null);

            if (bestNetScore != null) {
                // Create or update team hole score
                Optional<TeamHoleScore> existing = teamHoleScoreRepository
                    .findByRoundTeamIdAndHoleId(team.getId(), hole.getId());

                TeamHoleScore teamScore = existing.orElseGet(TeamHoleScore::new);
                teamScore.setRoundTeam(team);
                teamScore.setHole(hole);
                teamScore.setTournamentRound(teeTime.getTournamentRound());
                teamScore.setNetScore(bestNetScore);

                teamHoleScoreRepository.save(teamScore);
            }
        } catch (Exception e) {
            System.out.println("Error generating team hole score for team " + team.getId() + ", hole " + hole.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Calculate Stableford game points for teams based on net score vs par
     * Eagle: 8 pts, Birdie: 4 pts, Par: 2 pts, Bogey: 0 pts, Double Bogey or worse: -2 pts
     */
    private List<Integer> calculateStablefordGamePoints(List<TeamHoleScore> sortedByScore, Hole hole) {
        // First, calculate raw Stableford points for each team based on their score vs par
        List<Integer> stablefordPoints = new ArrayList<>();
        
        for (TeamHoleScore teamScore : sortedByScore) {
            int strokes = teamScore.getNetScore() != null ? teamScore.getNetScore() : 999;
            int par = hole.getPar() != null ? hole.getPar() : 4;
            int scoreDiff = strokes - par;

            int pts;
            if (scoreDiff <= -2) {
                // Eagle or better
                pts = 8;
            } else if (scoreDiff == -1) {
                // Birdie
                pts = 4;
            } else if (scoreDiff == 0) {
                // Par
                pts = 2;
            } else if (scoreDiff == 1) {
                // Bogey
                pts = 0;
            } else {
                // Double Bogey or worse
                pts = -2;
            }

            stablefordPoints.add(pts);
        }

        // Now rank teams by their Stableford points (highest to lowest)
        // and distribute competitive ranking points with tie handling
        List<Integer> rankingPoints = new ArrayList<>();
        int numTeams = sortedByScore.size();
        
        // Create list of (index, stablefordPoints) for ranking
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < numTeams; i++) {
            indices.add(i);
        }
        
        // Sort indices by stableford points descending (better scores first)
        indices.sort((i, j) -> Integer.compare(stablefordPoints.get(j), stablefordPoints.get(i)));
        
        // Assign ranking points based on position, handling ties
        int rank = 1;
        for (int i = 0; i < numTeams; ) {
            // Find all teams tied at this rank
            int currentScore = stablefordPoints.get(indices.get(i));
            List<Integer> tiedIndices = new ArrayList<>();
            tiedIndices.add(i);
            
            while (i + tiedIndices.size() < numTeams && 
                   stablefordPoints.get(indices.get(i + tiedIndices.size())).equals(currentScore)) {
                tiedIndices.add(i + tiedIndices.size());
            }
            
            // Calculate average points for ties
            // Points for this rank: numTeams - rank + 1
            double pointsForRank = numTeams - rank + 1;
            double pointsForNextRank = numTeams - (rank + tiedIndices.size()) + 1;
            double avgPoints = (pointsForRank + pointsForNextRank - 1) / 2.0;
            
            // Assign to all tied teams (round to nearest integer, or could use half-points)
            for (Integer idx : tiedIndices) {
                rankingPoints.add((int) Math.round(avgPoints));
            }
            
            rank += tiedIndices.size();
            i += tiedIndices.size();
        }

        // Create result list in original order
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < numTeams; i++) {
            result.add(0);
        }
        
        for (int i = 0; i < numTeams; i++) {
            result.set(indices.get(i), rankingPoints.get(i));
        }

        return result;
    }


    /**
     * Calculate Stableford game points for players within each team
     * Each player competes within their team using same Stableford scoring
     */
    private void calculatePlayerStablefordPointsForHole(List<RoundTeam> allTeams, Hole hole) {
        try {
            for (RoundTeam team : allTeams) {
                // Get all players on this team
                List<Long> teamPlayerIds = new java.util.ArrayList<>();
                if (team.getPlayer1Id() != null && team.getPlayer1Id() > 0) {
                    teamPlayerIds.add(team.getPlayer1Id());
                }
                if (team.getPlayer2Id() != null && team.getPlayer2Id() > 0) {
                    teamPlayerIds.add(team.getPlayer2Id());
                }
                if (team.getPlayer3Id() != null && team.getPlayer3Id() > 0) {
                    teamPlayerIds.add(team.getPlayer3Id());
                }

                if (teamPlayerIds.isEmpty()) {
                    continue; // No players on this team
                }

                // Get scorecards for all team members on this hole
                List<PlayerScorecard> teamScorecardsForHole = new java.util.ArrayList<>();

                for (Long playerId : teamPlayerIds) {
                    Optional<PlayerScorecard> scorecard = playerScorecardRepository
                        .findByRoundTeeTimeIdAndPlayerIdAndHoleId(team.getRoundTeeTime().getId(), playerId, hole.getId());
                    
                    if (scorecard.isPresent() && scorecard.get().getNetScore() != null) {
                        teamScorecardsForHole.add(scorecard.get());
                    }
                }

                // Need at least 1 player scorecard to assign points
                if (teamScorecardsForHole.isEmpty()) {
                    continue;
                }

                // Calculate Stableford points for each player
                for (PlayerScorecard scorecard : teamScorecardsForHole) {
                    int strokes = scorecard.getNetScore();
                    int par = hole.getPar() != null ? hole.getPar() : 4;
                    int scoreDiff = strokes - par;

                    int pts;
                    if (scoreDiff <= -2) {
                        // Eagle or better = 8 pts
                        pts = 8;
                    } else if (scoreDiff == -1) {
                        // Birdie = 4 pts
                        pts = 4;
                    } else if (scoreDiff == 0) {
                        // Par = 2 pts
                        pts = 2;
                    } else if (scoreDiff == 1) {
                        // Bogey = 0 pts
                        pts = 0;
                    } else {
                        // Double Bogey or worse = -2 pts
                        pts = -2;
                    }

                    scorecard.setGamePoints(pts);
                    playerScorecardRepository.save(scorecard);
                }
            }
        } catch (Exception e) {
            // Log error but don't throw - continue processing other teams
        }
    }

    /**
     * Calculate game points using Nines rules
     * Assumes list is sorted by net score (ascending = best first)
     */
    private List<Integer> calculateGamePoints(List<TeamHoleScore> sortedByScore) {
        List<Integer> points = new ArrayList<>();

        for (TeamHoleScore score : sortedByScore) {
            points.add(0); // Placeholder
        }

        int numTeams = sortedByScore.size();

        // Only support 3-team scoring for now (Nines)
        if (numTeams != 3) {
            // Equal distribution if not 3 teams
            int pointsPerTeam = 9 / numTeams;
            for (int i = 0; i < numTeams; i++) {
                points.set(i, pointsPerTeam);
            }
            return points;
        }

        // 3-team Nines scoring
        Integer score1 = sortedByScore.get(0).getNetScore();
        Integer score2 = sortedByScore.get(1).getNetScore();
        Integer score3 = sortedByScore.get(2).getNetScore();

        // All tied
        if (score1.equals(score2) && score2.equals(score3)) {
            return Arrays.asList(3, 3, 3);
        }

        // Tie for first place (1st and 2nd tied, 3rd different)
        if (score1.equals(score2) && !score2.equals(score3)) {
            return Arrays.asList(4, 4, 1);
        }

        // Tie for second place (1st different, 2nd and 3rd tied)
        if (!score1.equals(score2) && score2.equals(score3)) {
            return Arrays.asList(5, 2, 2);
        }

        // Standard: no ties
        return Arrays.asList(5, 3, 1);
    }

    /**
     * Calculate stroke-based team rankings
     * For games where scoring type = "stroke" (like 2-Man Aggregate)
     * Ranks teams by total net score (lowest = best = highest points)
     * Handles ties by splitting points evenly
     * 
     * @param tournamentRoundId The round to rank
     * @return Map of teamId -> ranking points
     */
    public Map<Long, Double> calculateStrokeRankings(Long tournamentRoundId) {
        Map<Long, Double> teamRankings = new HashMap<>();
        
        try {
            // Get all tee times for this round
            List<RoundTeeTime> roundTeeTimes = roundTeeTimeRepository.findAllByTournamentRoundId(tournamentRoundId);
            if (roundTeeTimes.isEmpty()) {
                System.out.println("No tee times found for round " + tournamentRoundId);
                return teamRankings;
            }

            // Collect all teams and calculate their totals
            Map<Long, Integer> teamTotals = new HashMap<>();
            Map<Long, RoundTeam> teamMap = new HashMap<>();

            for (RoundTeeTime teeTime : roundTeeTimes) {
                List<RoundTeam> teams = roundTeamRepository.findByRoundTeeTimeId(teeTime.getId());
                
                for (RoundTeam team : teams) {
                    teamMap.put(team.getId(), team);
                    
                    // Sum all net scores for this team
                    List<PlayerScorecard> teamScorecards = playerScorecardRepository.findByRoundTeeTimeId(teeTime.getId()).stream()
                        .filter(sc -> isPlayerOnTeam(team, sc.getPlayer().getId()))
                        .collect(Collectors.toList());

                    int teamTotal = teamScorecards.stream()
                        .mapToInt(sc -> sc.getNetScore() != null ? sc.getNetScore() : 0)
                        .sum();

                    teamTotals.put(team.getId(), teamTotal);
                }
            }

            if (teamTotals.isEmpty()) {
                System.out.println("No teams found for round " + tournamentRoundId);
                return teamRankings;
            }

            // Sort teams by total score (ascending = best)
            List<Map.Entry<Long, Integer>> sortedTeams = teamTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toList());

            int numTeams = sortedTeams.size();
            int position = 0;
            int i = 0;

            while (i < sortedTeams.size()) {
                int currentScore = sortedTeams.get(i).getValue();
                List<Long> tiedTeams = new ArrayList<>();
                
                // Find all teams with the same score
                while (i < sortedTeams.size() && sortedTeams.get(i).getValue() == currentScore) {
                    tiedTeams.add(sortedTeams.get(i).getKey());
                    i++;
                }

                // Calculate points for this rank
                // Points go from numTeams (1st place) down to 1 (last place)
                // Position is 0-based, so 1st place is at position 0, gets numTeams points
                int rankStart = numTeams - position;
                int rankEnd = numTeams - (position + tiedTeams.size() - 1);
                
                // Average the points for tied teams
                double avgPoints = (rankStart + rankEnd) / 2.0;
                
                System.out.println("Stroke Ranking - Position " + (position + 1) + ": " + 
                    tiedTeams.size() + " team(s) with score " + currentScore + 
                    " get " + avgPoints + " points each");

                for (Long teamId : tiedTeams) {
                    teamRankings.put(teamId, avgPoints);
                }

                position += tiedTeams.size();
            }

            System.out.println("========== Stroke Rankings Complete ==========");
            System.out.println("Rankings: " + teamRankings);
            return teamRankings;

        } catch (Exception e) {
            System.out.println("Error calculating stroke rankings: " + e.getMessage());
            e.printStackTrace();
        }

        return teamRankings;
    }

    /**
     * Helper method to check if a player belongs to a team
     */
    private boolean isPlayerOnTeam(RoundTeam team, Long playerId) {
        return (team.getPlayer1Id() != null && team.getPlayer1Id().equals(playerId)) ||
               (team.getPlayer2Id() != null && team.getPlayer2Id().equals(playerId)) ||
               (team.getPlayer3Id() != null && team.getPlayer3Id().equals(playerId));
    }

    /**
     * Validate that ALL teams in a round have complete scorecards for ALL 18 holes
     */
    private void validateAllTeamsHaveCompleteScorecardsForAllHolesRoundLevel(List<RoundTeam> allTeams, List<Hole> holes) {
        StringBuilder missingScores = new StringBuilder();
        
        for (RoundTeam team : allTeams) {
            List<Long> teamPlayerIds = new java.util.ArrayList<>();
            if (team.getPlayer1Id() != null) teamPlayerIds.add(team.getPlayer1Id());
            if (team.getPlayer2Id() != null) teamPlayerIds.add(team.getPlayer2Id());
            if (team.getPlayer3Id() != null) teamPlayerIds.add(team.getPlayer3Id());
            
            if (teamPlayerIds.isEmpty()) {
                throw new IllegalStateException("Team " + team.getId() + " has no players assigned");
            }
            
            RoundTeeTime teeTime = team.getRoundTeeTime();
            
            // Check each hole - team must have at least ONE player score for each hole
            for (Hole hole : holes) {
                boolean hasScoreForHole = false;
                
                for (Long playerId : teamPlayerIds) {
                    Optional<PlayerScorecard> scorecard = playerScorecardRepository
                        .findByRoundTeeTimeIdAndPlayerIdAndHoleId(teeTime.getId(), playerId, hole.getId());
                    
                    if (scorecard.isPresent() && scorecard.get().getGrossScore() != null) {
                        hasScoreForHole = true;
                        break;
                    }
                }
                
                if (!hasScoreForHole) {
                    missingScores.append("Team ")
                        .append(team.getId())
                        .append(" is missing score for hole ")
                        .append(hole.getHoleNumber())
                        .append(". ");
                }
            }
        }
        
        if (missingScores.length() > 0) {
            throw new IllegalStateException("Cannot calculate team competition. " + missingScores.toString() +
                    "All " + allTeams.size() + " teams must have complete scores for all 18 holes before team game points can be calculated.");
        }
    }
}
