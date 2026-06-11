package com.degen.backend.service;

import com.degen.backend.dto.LeaderboardEntryDto;
import com.degen.backend.dto.RoundLeaderboardEntryDto;
import com.degen.backend.entity.*;
import com.degen.backend.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private final TournamentRoundRepository tournamentRoundRepository;
    private final RoundTeeTimeRepository roundTeeTimeRepository;
    private final PlayerScorecardRepository playerScorecardRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final TeamHoleScoreRepository teamHoleScoreRepository;
    private final PlayerRepository playerRepository;

    public LeaderboardService(
            TournamentRoundRepository tournamentRoundRepository,
            RoundTeeTimeRepository roundTeeTimeRepository,
            PlayerScorecardRepository playerScorecardRepository,
            RoundTeamRepository roundTeamRepository,
            TeamHoleScoreRepository teamHoleScoreRepository,
            PlayerRepository playerRepository) {
        this.tournamentRoundRepository = tournamentRoundRepository;
        this.roundTeeTimeRepository = roundTeeTimeRepository;
        this.playerScorecardRepository = playerScorecardRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.teamHoleScoreRepository = teamHoleScoreRepository;
        this.playerRepository = playerRepository;
    }

    /**
     * Calculate tournament leaderboard for all rounds
     * Points include:
     * 1. Tee-time individual ranking: 3 for 1st, 2 for 2nd, 1 for 3rd
     * 2. Inter-group team ranking: 3 for 1st team, 2 for 2nd, 1 for 3rd
     * 3. Individual games: For Split Skins, use total game_points; for Stroke, use
     * net scores
     * Ties are handled by averaging points
     */
    public List<LeaderboardEntryDto> getTournamentLeaderboard(Long tournamentId) {
        System.out.println("\n===== getTournamentLeaderboard called for tournament " + tournamentId + " =====");
        // Get all rounds for tournament
        List<TournamentRound> rounds = tournamentRoundRepository.findByTournamentId(tournamentId);

        System.out.println("Found " + rounds.size() + " rounds for tournament " + tournamentId);

        if (rounds.isEmpty()) {
            System.out.println("No rounds found - returning empty leaderboard");
            return new ArrayList<>();
        }

        // Map to store player leaderboard entries
        Map<Long, LeaderboardEntryDto> leaderboard = new HashMap<>();

        // Process each round
        for (TournamentRound round : rounds) {
            System.out.println("Processing round " + round.getId());

            // Get all tee times for this round
            List<RoundTeeTime> teeTimes = roundTeeTimeRepository.findAllByTournamentRoundId(round.getId());

            System.out.println("  Found " + teeTimes.size() + " tee times for round " + round.getId());

            if (teeTimes.isEmpty()) {
                System.out.println("  No tee times - skipping round");
                continue; // Skip rounds with no tee times
            }

            // Check if this round has any scores at all
            boolean hasAnyScores = teeTimes.stream()
                    .flatMap(teeTime -> playerScorecardRepository.findByRoundTeeTimeId(teeTime.getId()).stream())
                    .anyMatch(sc -> sc.getGrossScore() != null || sc.getNetScore() != null);

            if (!hasAnyScores) {
                System.out.println("  Round " + round.getId() + " has no scores entered - skipping from leaderboard");
                continue; // Skip rounds with no scores entered
            }

            System.out.println("  Round " + round.getId() + " has scores - processing");

            // Check if this is an Individual game
            if (round.getGame() != null && round.getGame().getId() == 4L) {
                // Individual game - handle separately
                processIndividualGameRound(round, teeTimes, leaderboard);
            } else {
                // Team-based game - original logic
                // Step 1: Calculate individual tee-time points
                for (RoundTeeTime teeTime : teeTimes) {
                    List<PlayerScorecard> scorecards = playerScorecardRepository.findByRoundTeeTimeId(teeTime.getId());

                    // Group by player and sum game points for this tee time
                    Map<Long, Integer> playerTeeTimeScores = scorecards.stream()
                            .filter(sc -> sc.getGamePoints() != null)
                            .collect(Collectors.groupingBy(
                                    sc -> sc.getPlayer().getId(),
                                    Collectors.summingInt(PlayerScorecard::getGamePoints)));

                    // Rank players and assign tee-time points
                    assignTeeTimePoints(playerTeeTimeScores, scorecards, leaderboard, round.getId());
                }

                // Step 2: Calculate inter-group team points
                assignTeamPoints(teeTimes, leaderboard, round.getId(), round);
            }
        }

        System.out.println("Leaderboard has " + leaderboard.size() + " entries");
        List<LeaderboardEntryDto> result = leaderboard.values().stream()
                .sorted(Comparator.comparing(LeaderboardEntryDto::getTotalPoints).reversed())
                .collect(Collectors.toList());

        System.out.println("Returning " + result.size() + " leaderboard entries");
        return result;
    }

    /**
     * Process an Individual game round
     * For Stroke Individual: Rank by total net score
     * For Split Skins Individual: Rank by total game_points
     */
    private void processIndividualGameRound(TournamentRound round, List<RoundTeeTime> teeTimes,
            Map<Long, LeaderboardEntryDto> leaderboard) {
        Long scoringTypeId = (round.getScoringType() != null)
                ? round.getScoringType().getId()
                : null;

        // Get all player scorecards for this round
        Map<Long, List<PlayerScorecard>> playerScorecards = new HashMap<>();
        Map<Long, String> playerNames = new HashMap<>();

        for (RoundTeeTime teeTime : teeTimes) {
            List<PlayerScorecard> scorecards = playerScorecardRepository.findByRoundTeeTimeId(teeTime.getId());
            for (PlayerScorecard sc : scorecards) {
                Long playerId = sc.getPlayer().getId();
                playerScorecards.computeIfAbsent(playerId, k -> new ArrayList<>()).add(sc);
                playerNames.put(playerId, sc.getPlayer().getFirstName() + " " + sc.getPlayer().getLastName());
            }
        }

        if (playerScorecards.isEmpty()) {
            return;
        }

        if (scoringTypeId != null && scoringTypeId == 5L) {
            // Split Skins Individual: Rank by total game_points (stored as cents)
            Map<Long, Double> playerGamePoints = new HashMap<>();
            for (Map.Entry<Long, List<PlayerScorecard>> entry : playerScorecards.entrySet()) {
                Long playerId = entry.getKey();
                double totalPoints = entry.getValue().stream()
                        .filter(sc -> sc.getGamePoints() != null)
                        .mapToDouble(sc -> sc.getGamePoints() / 100.0) // Convert from cents
                        .sum();
                playerGamePoints.put(playerId, totalPoints);
            }

            // Rank players by game points (descending)
            List<Map.Entry<Long, Double>> sortedPlayers = playerGamePoints.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .toList();

            // Assign points: top player gets all their points, others get proportional
            for (Map.Entry<Long, Double> entry : sortedPlayers) {
                Long playerId = entry.getKey();
                LeaderboardEntryDto leaderboardEntry = leaderboard.computeIfAbsent(playerId,
                        pId -> new LeaderboardEntryDto(pId, playerNames.get(pId)));
                leaderboardEntry.addRoundPoints(round.getId(), entry.getValue());
            }
        } else {
            // Stroke Individual (or default): Rank by total net score (ascending/lowest
            // first)
            Map<Long, Integer> playerNetScores = new HashMap<>();
            for (Map.Entry<Long, List<PlayerScorecard>> entry : playerScorecards.entrySet()) {
                Long playerId = entry.getKey();
                int totalNet = entry.getValue().stream()
                        .filter(sc -> sc.getNetScore() != null)
                        .mapToInt(PlayerScorecard::getNetScore)
                        .sum();
                playerNetScores.put(playerId, totalNet);
            }

            // Rank players by net score (ascending - lowest/best first)
            List<Map.Entry<Long, Integer>> sortedPlayers = playerNetScores.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .toList();

            // Assign points: 1st gets numPlayers points, down to 1 for last
            int numPlayers = sortedPlayers.size();
            int currentRank = 0;

            while (currentRank < sortedPlayers.size()) {
                Integer currentScore = sortedPlayers.get(currentRank).getValue();
                List<Integer> tiedIndices = new ArrayList<>();
                tiedIndices.add(currentRank);

                int nextRank = currentRank + 1;
                while (nextRank < sortedPlayers.size() &&
                        sortedPlayers.get(nextRank).getValue().equals(currentScore)) {
                    tiedIndices.add(nextRank);
                    nextRank++;
                }

                // Calculate average points for tied players
                int pointsForBest = numPlayers - currentRank;
                int pointsForWorst = numPlayers - (currentRank + tiedIndices.size() - 1);
                double avgPoints = (pointsForBest + pointsForWorst) / 2.0;

                for (int idx : tiedIndices) {
                    Long playerId = sortedPlayers.get(idx).getKey();
                    LeaderboardEntryDto entry = leaderboard.computeIfAbsent(playerId,
                            pId -> new LeaderboardEntryDto(pId, playerNames.get(pId)));
                    entry.addRoundPoints(round.getId(), avgPoints);
                }

                currentRank = nextRank;
            }
        }
    }

    /**
     * Rank players within a tee time and assign points
     * Points: 3 for 1st, 2 for 2nd, 1 for 3rd
     * Ties are handled by averaging points
     */
    private void assignTeeTimePoints(
            Map<Long, Integer> playerScores,
            List<PlayerScorecard> scorecards,
            Map<Long, LeaderboardEntryDto> leaderboard,
            Long roundId) {

        // Map to store player names
        Map<Long, String> playerNames = new HashMap<>();
        scorecards.forEach(sc -> playerNames.put(sc.getPlayer().getId(),
                sc.getPlayer().getFirstName() + " " + sc.getPlayer().getLastName()));

        // Sort players by their score (highest first)
        List<Map.Entry<Long, Integer>> sortedPlayers = playerScores.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue())) // Descending
                .toList();

        // Points array: [3, 2, 1] for top 3 finishers
        int[] pointsArray = { 3, 2, 1 };

        int currentRank = 0;
        int pointsIndex = 0;

        while (currentRank < sortedPlayers.size() && pointsIndex < pointsArray.length) {
            // Find all players tied at current rank
            Integer currentScore = sortedPlayers.get(currentRank).getValue();
            List<Integer> tiedIndices = new ArrayList<>();
            tiedIndices.add(currentRank);

            int nextRank = currentRank + 1;
            while (nextRank < sortedPlayers.size() &&
                    sortedPlayers.get(nextRank).getValue().equals(currentScore)) {
                tiedIndices.add(nextRank);
                nextRank++;
            }

            // Calculate average points for tied players
            double pointsToAward = pointsArray[pointsIndex];
            if (tiedIndices.size() > 1) {
                // Average the points for this rank and next ones
                int pointsSum = 0;
                for (int i = 0; i < tiedIndices.size() && pointsIndex < pointsArray.length; i++) {
                    pointsSum += pointsArray[pointsIndex];
                    pointsIndex++;
                }
                pointsToAward = (double) pointsSum / tiedIndices.size();
            } else {
                pointsIndex++;
            }

            // Assign points to tied players
            for (int idx : tiedIndices) {
                Long playerId = sortedPlayers.get(idx).getKey();

                LeaderboardEntryDto entry = leaderboard.computeIfAbsent(playerId,
                        pId -> new LeaderboardEntryDto(pId, playerNames.get(pId)));

                entry.addRoundPoints(roundId, pointsToAward);
            }

            currentRank = nextRank;
        }
    }

    /**
     * Calculate and assign inter-group team points to players
     * Teams compete across all tee times in a round
     * For Nines: Points: 3 for best team, 2 for 2nd best, 1 for 3rd best (based on
     * gamePoints)
     * For Stroke: Points based on net scores (best/lowest gets most points)
     */
    private void assignTeamPoints(List<RoundTeeTime> teeTimes,
            Map<Long, LeaderboardEntryDto> leaderboard,
            Long roundId,
            TournamentRound round) {
        try {
            if (teeTimes.isEmpty()) {
                return;
            }

            // Check if this is a stroke round (scoring_type_id = 1 for Stroke)
            Long scoringTypeId = (round.getScoringType() != null)
                    ? round.getScoringType().getId()
                    : null;
            boolean isStrokeRound = scoringTypeId != null && scoringTypeId == 1;

            System.out.println(
                    "DEBUG: Round " + roundId + " scoringTypeId=" + scoringTypeId + " isStrokeRound=" + isStrokeRound);

            // Collect all teams in this round and their scores
            Map<Long, Integer> teamScores = new HashMap<>();
            Map<Long, RoundTeam> teamMap = new HashMap<>();
            Map<Long, List<Long>> teamPlayers = new HashMap<>();
            Map<Long, String> playerNames = new HashMap<>(); // To create leaderboard entries

            for (RoundTeeTime teeTime : teeTimes) {
                List<RoundTeam> teams = roundTeamRepository.findByRoundTeeTimeId(teeTime.getId());

                for (RoundTeam team : teams) {
                    List<TeamHoleScore> teamHoleScores = teamHoleScoreRepository.findByRoundTeamId(team.getId());

                    int teamScore;
                    if (isStrokeRound) {
                        // For stroke rounds, sum net scores
                        teamScore = teamHoleScores.stream()
                                .map(TeamHoleScore::getNetScore)
                                .filter(Objects::nonNull)
                                .mapToInt(Integer::intValue)
                                .sum();
                    } else {
                        // For Nines, sum game points
                        teamScore = teamHoleScores.stream()
                                .filter(ths -> ths.getGamePoints() != null)
                                .mapToInt(TeamHoleScore::getGamePoints)
                                .sum();
                    }

                    teamScores.put(team.getId(), teamScore);
                    teamMap.put(team.getId(), team);

                    // Track which players are on this team
                    List<Long> playerIds = new ArrayList<>();
                    if (team.getPlayer1Id() != null)
                        playerIds.add(team.getPlayer1Id());
                    if (team.getPlayer2Id() != null)
                        playerIds.add(team.getPlayer2Id());
                    if (team.getPlayer3Id() != null)
                        playerIds.add(team.getPlayer3Id());
                    teamPlayers.put(team.getId(), playerIds);

                    // Populate player names map
                    for (Long playerId : playerIds) {
                        if (!playerNames.containsKey(playerId)) {
                            Optional<Player> playerOpt = playerRepository.findById(playerId);
                            playerOpt.ifPresent(player -> playerNames.put(playerId,
                                    player.getFirstName() + " " + player.getLastName()));
                        }
                    }
                }
            }

            if (teamScores.isEmpty()) {
                System.out.println("  No team scores found!");
                return;
            }

            // Check if all teams have 0 score (no actual scores entered)
            boolean hasAnyScore = teamScores.values().stream().anyMatch(score -> score > 0);
            if (!hasAnyScore) {
                System.out.println("  No actual scores entered for any team - skipping team points calculation");
                return;
            }

            System.out.println("  Team scores: " + teamScores);

            // Rank teams by score
            List<Map.Entry<Long, Integer>> sortedTeams;
            if (isStrokeRound) {
                // For stroke, ascending (lowest/best score first)
                sortedTeams = teamScores.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue())
                        .collect(Collectors.toList());
            } else {
                // For Nines, descending (highest/best score first)
                sortedTeams = teamScores.entrySet().stream()
                        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                        .collect(Collectors.toList());
            }

            int numTeams = sortedTeams.size();

            if (isStrokeRound) {
                System.out.println("  Using STROKE ranking");
                // For stroke rounds: assign points where 1st place gets numTeams points, last
                // gets 1
                int currentRank = 0;

                while (currentRank < sortedTeams.size()) {
                    // Find all teams tied at current rank
                    Integer currentScore = sortedTeams.get(currentRank).getValue();
                    List<Integer> tiedIndices = new ArrayList<>();
                    tiedIndices.add(currentRank);

                    int nextRank = currentRank + 1;
                    while (nextRank < sortedTeams.size() &&
                            sortedTeams.get(nextRank).getValue().equals(currentScore)) {
                        tiedIndices.add(nextRank);
                        nextRank++;
                    }

                    // Calculate points for this rank
                    int pointsForBest = numTeams - currentRank;
                    int pointsForWorst = numTeams - (currentRank + tiedIndices.size() - 1);
                    double avgPoints = (pointsForBest + pointsForWorst) / 2.0;

                    System.out.println("    Rank " + (currentRank + 1) + ": " + tiedIndices.size()
                            + " teams with score " + currentScore + ", assigning " + avgPoints + " points each");

                    // Award points to all players on the tied teams
                    for (int idx : tiedIndices) {
                        Long teamId = sortedTeams.get(idx).getKey();
                        List<Long> playerIds = teamPlayers.get(teamId);

                        if (playerIds != null) {
                            for (Long playerId : playerIds) {
                                LeaderboardEntryDto entry = leaderboard.computeIfAbsent(playerId,
                                        pId -> new LeaderboardEntryDto(pId, playerNames.get(pId)));
                                entry.addRoundPoints(roundId, avgPoints);
                            }
                        }
                    }

                    currentRank = nextRank;
                }
            } else {
                System.out.println("  Using NINES ranking (gamePoints)");
                // For Nines/Stableford: points scale based on number of teams
                // 1st place gets numTeams points, 2nd gets numTeams-1, etc. down to 1
                int currentRank = 0;

                while (currentRank < sortedTeams.size()) {
                    // Find all teams tied at current rank
                    Integer currentScore = sortedTeams.get(currentRank).getValue();
                    List<Integer> tiedIndices = new ArrayList<>();
                    tiedIndices.add(currentRank);

                    int nextRank = currentRank + 1;
                    while (nextRank < sortedTeams.size() &&
                            sortedTeams.get(nextRank).getValue().equals(currentScore)) {
                        tiedIndices.add(nextRank);
                        nextRank++;
                    }

                    // Calculate average points for tied teams
                    // Points for 1st place: numTeams
                    // Points for last place: 1
                    double pointsForBest = numTeams - currentRank;
                    double pointsForWorst = numTeams - (currentRank + tiedIndices.size() - 1);
                    double avgPoints = (pointsForBest + pointsForWorst) / 2.0;

                    System.out.println(
                            "    Rank " + (currentRank + 1) + ": " + tiedIndices.size() + " teams with gamePoints "
                                    + currentScore + ", assigning " + avgPoints + " points each");

                    // Award points to all players on the tied teams
                    for (int idx : tiedIndices) {
                        Long teamId = sortedTeams.get(idx).getKey();
                        List<Long> playerIds = teamPlayers.get(teamId);

                        if (playerIds != null) {
                            for (Long playerId : playerIds) {
                                LeaderboardEntryDto entry = leaderboard.computeIfAbsent(playerId,
                                        pId -> new LeaderboardEntryDto(pId, playerNames.get(pId)));
                                entry.addRoundPoints(roundId, avgPoints);
                            }
                        }
                    }

                    currentRank = nextRank;
                }
            }

            // Step 3: If vs_group is enabled, rank players within each tee time
            if (round.getVsGroup() != null && round.getVsGroup()) {
                System.out.println("  vsGroup enabled - calculating player points within tee times");
                for (RoundTeeTime teeTime : teeTimes) {
                    assignPlayerPointsWithinTeeTime(teeTime, leaderboard, roundId);
                }
            }

        } catch (Exception e) {
            System.err.println("ERROR in assignTeamPoints: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Rank all players within a tee time and assign vs_group points
     * All players in the tee time compete against each other
     * Points scale: 2.0, 1.5, 1.0, 0.5
     * Ties: Average the points for tied positions
     */
    private void assignPlayerPointsWithinTeeTime(RoundTeeTime teeTime, Map<Long, LeaderboardEntryDto> leaderboard,
            Long roundId) {
        try {
            // Get all teams in this tee time
            List<RoundTeam> teamsInTeeTime = roundTeamRepository.findByRoundTeeTimeId(teeTime.getId());
            if (teamsInTeeTime.isEmpty()) {
                return;
            }

            // Collect all players and their net scores for this tee time
            Map<Long, Integer> playerNetScores = new HashMap<>();
            Map<Long, String> playerNames = new HashMap<>();

            for (RoundTeam team : teamsInTeeTime) {
                List<Long> teamPlayerIds = new ArrayList<>();
                if (team.getPlayer1Id() != null && team.getPlayer1Id() > 0)
                    teamPlayerIds.add(team.getPlayer1Id());
                if (team.getPlayer2Id() != null && team.getPlayer2Id() > 0)
                    teamPlayerIds.add(team.getPlayer2Id());
                if (team.getPlayer3Id() != null && team.getPlayer3Id() > 0)
                    teamPlayerIds.add(team.getPlayer3Id());

                for (Long playerId : teamPlayerIds) {
                    // Store player name
                    if (!playerNames.containsKey(playerId)) {
                        Optional<Player> playerOpt = playerRepository.findById(playerId);
                        playerOpt.ifPresent(player -> playerNames.put(playerId,
                                player.getFirstName() + " " + player.getLastName()));
                    }

                    // Get net score for this player in this tee time
                    List<PlayerScorecard> playerScorecards = playerScorecardRepository
                            .findByRoundTeeTimeIdAndPlayerId(teeTime.getId(), playerId);

                    int totalNetScore = playerScorecards.stream()
                            .filter(sc -> sc.getNetScore() != null)
                            .mapToInt(PlayerScorecard::getNetScore)
                            .sum();

                    if (totalNetScore > 0) {
                        playerNetScores.put(playerId, totalNetScore);
                    }
                }
            }

            if (playerNetScores.size() < 2) {
                return; // Need at least 2 players with scores for vs_group
            }

            // Sort players by net score (ascending = best first)
            List<Map.Entry<Long, Integer>> sortedPlayers = playerNetScores.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .toList();

            // vs_group points scale: 2.0, 1.5, 1.0, 0.5
            double[] vsGroupPointsArray = { 2.0, 1.5, 1.0, 0.5 };

            int currentRank = 0;
            int pointsIndex = 0;

            System.out.println("  Tee Time " + teeTime.getId() + ": ranking " + sortedPlayers.size() + " players");

            while (currentRank < sortedPlayers.size() && pointsIndex < vsGroupPointsArray.length) {
                // Find all players tied at current rank
                Integer currentScore = sortedPlayers.get(currentRank).getValue();
                List<Integer> tiedIndices = new ArrayList<>();
                tiedIndices.add(currentRank);

                int nextRank = currentRank + 1;
                while (nextRank < sortedPlayers.size() &&
                        sortedPlayers.get(nextRank).getValue().equals(currentScore)) {
                    tiedIndices.add(nextRank);
                    nextRank++;
                }

                // Calculate average points for tied players
                double pointsSum = 0.0;
                int pointsUsed = 0;
                for (int i = 0; i < tiedIndices.size() && pointsIndex < vsGroupPointsArray.length; i++) {
                    pointsSum += vsGroupPointsArray[pointsIndex];
                    pointsIndex++;
                    pointsUsed++;
                }
                double avgPoints = pointsSum / pointsUsed;

                System.out.println("    Rank " + (currentRank + 1) + ": " + tiedIndices.size() + " players with score "
                        + currentScore + ", assigning " + avgPoints + " points each");

                // Award points to all tied players
                for (int idx : tiedIndices) {
                    Long playerId = sortedPlayers.get(idx).getKey();
                    LeaderboardEntryDto entry = leaderboard.computeIfAbsent(playerId,
                            pId -> new LeaderboardEntryDto(pId, playerNames.get(pId)));
                    entry.addRoundPoints(roundId, avgPoints);
                }

                currentRank = nextRank;
            }
        } catch (Exception e) {
            System.err
                    .println("Error calculating player points for tee time " + teeTime.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get leaderboard for a specific round with live scoring data
     * Returns players sorted by score with their current progress
     * totalPoints = cumulative points up to and including this round (snapshot in
     * time)
     */
    public List<RoundLeaderboardEntryDto> getRoundLeaderboard(Long roundId) {
        System.out.println("\n===== getRoundLeaderboard called for round " + roundId + " =====");

        // Get the round to find the tournament and ordering
        TournamentRound round = tournamentRoundRepository.findById(roundId).orElse(null);
        if (round == null) {
            System.out.println("Round not found");
            return new ArrayList<>();
        }

        // Get all rounds for this tournament, sorted by day
        List<TournamentRound> allRounds = tournamentRoundRepository.findByTournamentId(round.getTournament().getId());
        allRounds.sort((a, b) -> a.getDay().compareTo(b.getDay()));

        // Find the index of the current round
        int currentRoundIndex = -1;
        for (int i = 0; i < allRounds.size(); i++) {
            if (allRounds.get(i).getId().equals(roundId)) {
                currentRoundIndex = i;
                break;
            }
        }

        if (currentRoundIndex == -1) {
            System.out.println("Round not found in tournament");
            return new ArrayList<>();
        }

        // Get rounds up to and including current round
        List<TournamentRound> roundsUpToCurrent = allRounds.subList(0, currentRoundIndex + 1);
        System.out.println("Calculating cumulative points through round " + currentRoundIndex + " ("
                + roundsUpToCurrent.size() + " rounds)");

        // Calculate tournament leaderboard (this will give us all round points)
        List<LeaderboardEntryDto> tournamentLeaderboard = getTournamentLeaderboard(round.getTournament().getId());
        Map<Long, LeaderboardEntryDto> tournamentLeaderboardMap = new HashMap<>();
        for (LeaderboardEntryDto entry : tournamentLeaderboard) {
            tournamentLeaderboardMap.put(entry.getPlayerId(), entry);
        }

        // Get all tee times for this round
        List<RoundTeeTime> teeTimes = roundTeeTimeRepository.findAllByTournamentRoundId(roundId);

        if (teeTimes.isEmpty()) {
            System.out.println("No tee times found for round");
            return new ArrayList<>();
        }

        // Collect all players in this round and their scores
        Map<Long, Integer> playerScoresRelativeToPar = new HashMap<>();
        Map<Long, Integer> playerTotalPar = new HashMap<>();
        Map<Long, Set<Long>> playerHolesCompleted = new HashMap<>();
        Map<Long, String> playerNames = new HashMap<>();

        for (RoundTeeTime teeTime : teeTimes) {
            List<PlayerScorecard> scorecards = playerScorecardRepository.findByRoundTeeTimeId(teeTime.getId());

            for (PlayerScorecard scorecard : scorecards) {
                Long playerId = scorecard.getPlayer().getId();

                // Store player name
                if (!playerNames.containsKey(playerId)) {
                    playerNames.put(playerId,
                            scorecard.getPlayer().getFirstName() + " " + scorecard.getPlayer().getLastName());
                }

                // Accumulate net score and par for this hole
                if (scorecard.getNetScore() != null) {
                    playerScoresRelativeToPar.put(playerId,
                            playerScoresRelativeToPar.getOrDefault(playerId, 0) + scorecard.getNetScore());
                }

                if (scorecard.getHole() != null && scorecard.getHole().getPar() != null) {
                    playerTotalPar.put(playerId,
                            playerTotalPar.getOrDefault(playerId, 0) + scorecard.getHole().getPar());
                }

                // Track unique holes completed
                if (scorecard.getHole() != null) {
                    playerHolesCompleted.putIfAbsent(playerId, new HashSet<>());
                    playerHolesCompleted.get(playerId).add(scorecard.getHole().getId());
                }
            }
        }

        if (playerScoresRelativeToPar.isEmpty()) {
            System.out.println("No scores found for this round");
            return new ArrayList<>();
        }

        // Create round leaderboard entries
        List<RoundLeaderboardEntryDto> roundLeaderboard = new ArrayList<>();

        for (Long playerId : playerNames.keySet()) {
            Integer totalNetScore = playerScoresRelativeToPar.getOrDefault(playerId, 0);
            Integer totalPar = playerTotalPar.getOrDefault(playerId, 0);
            Integer scoreRelativeToPar = totalNetScore - totalPar; // e.g., 66 - 70 = -4
            Integer thru = playerHolesCompleted.containsKey(playerId) ? playerHolesCompleted.get(playerId).size() : 0;

            // Get round points from tournament leaderboard
            LeaderboardEntryDto tourEntry = tournamentLeaderboardMap.get(playerId);
            Double roundPoints = 0.0;
            Double cumulativePoints = 0.0;

            if (tourEntry != null) {
                Double points = tourEntry.getRoundPoints().getOrDefault(roundId, 0.0);
                roundPoints = points != null ? points : 0.0;

                // Calculate cumulative points: sum all round points up to and including current
                // round
                for (TournamentRound r : roundsUpToCurrent) {
                    Double roundPts = tourEntry.getRoundPoints().getOrDefault(r.getId(), 0.0);
                    cumulativePoints += (roundPts != null ? roundPts : 0.0);
                }
            }

            RoundLeaderboardEntryDto entry = new RoundLeaderboardEntryDto(
                    playerId,
                    playerNames.get(playerId),
                    scoreRelativeToPar,
                    thru,
                    roundPoints,
                    cumulativePoints);

            roundLeaderboard.add(entry);
        }

        // Sort by score relative to par (ascending = best first; -4 is better than +2)
        roundLeaderboard.sort((a, b) -> {
            if (a.getScore() == null && b.getScore() == null)
                return 0;
            if (a.getScore() == null)
                return 1;
            if (b.getScore() == null)
                return -1;
            return a.getScore().compareTo(b.getScore());
        });

        System.out.println("Round leaderboard has " + roundLeaderboard.size() + " entries");
        return roundLeaderboard;
    }
}
