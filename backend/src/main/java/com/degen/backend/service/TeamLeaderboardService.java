package com.degen.backend.service;

import com.degen.backend.dto.TeamLeaderboardEntryDto;
import com.degen.backend.entity.*;
import com.degen.backend.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeamLeaderboardService {
    
    private final TournamentRoundRepository tournamentRoundRepository;
    private final RoundTeeTimeRepository roundTeeTimeRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final TeamHoleScoreRepository teamHoleScoreRepository;
    private final PlayerRepository playerRepository;

    public TeamLeaderboardService(
            TournamentRoundRepository tournamentRoundRepository,
            RoundTeeTimeRepository roundTeeTimeRepository,
            RoundTeamRepository roundTeamRepository,
            TeamHoleScoreRepository teamHoleScoreRepository,
            PlayerRepository playerRepository) {
        this.tournamentRoundRepository = tournamentRoundRepository;
        this.roundTeeTimeRepository = roundTeeTimeRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.teamHoleScoreRepository = teamHoleScoreRepository;
        this.playerRepository = playerRepository;
    }

    /**
     * Calculate tournament team leaderboard for all rounds
     * Handles both Nines (game points) and Stroke (net score ranking) scoring types
     * All team members get the same points
     */
    public List<TeamLeaderboardEntryDto> getTournamentTeamLeaderboard(Long tournamentId) {
        System.out.println("\n========== getTournamentTeamLeaderboard for Tournament " + tournamentId + " ==========");
        // Get all rounds for tournament
        List<TournamentRound> rounds = tournamentRoundRepository.findByTournamentId(tournamentId);
        System.out.println("Found " + rounds.size() + " rounds");
        
        if (rounds.isEmpty()) {
            return new ArrayList<>();
        }

        // Map to store team leaderboard entries
        Map<Long, TeamLeaderboardEntryDto> leaderboard = new HashMap<>();

        // Process each round
        for (TournamentRound round : rounds) {
            // Check if this round uses stroke-based scoring
            String scoringTypeName = round.getGame() != null && round.getGame().getScoringType() != null 
                ? round.getGame().getScoringType().getScoringTypeName() 
                : "";

            System.out.println("Round " + round.getId() + " scoring type: " + scoringTypeName);
            
            if ("stroke".equalsIgnoreCase(scoringTypeName)) {
                // Use stroke ranking system
                System.out.println("  -> Using stroke ranking");
                processStrokeRound(round, leaderboard);
            } else {
                // Use Nines system (default)
                System.out.println("  -> Using Nines ranking");
                processNinesRound(round, leaderboard);
            }
        }

        // Sort by total points descending
        return leaderboard.values().stream()
            .sorted(Comparator.comparing(TeamLeaderboardEntryDto::getTotalPoints).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Process Nines round: sum game points for each team
     */
    private void processNinesRound(TournamentRound round, Map<Long, TeamLeaderboardEntryDto> leaderboard) {
        // Get all tee times for this round
        List<RoundTeeTime> teeTimes = roundTeeTimeRepository.findAllByTournamentRoundId(round.getId());

        // Process each tee time
        for (RoundTeeTime teeTime : teeTimes) {
            List<RoundTeam> teams = roundTeamRepository.findByRoundTeeTimeId(teeTime.getId());

            // Calculate team scores for this tee time
            Map<Long, Integer> teamTeeTimeScores = new HashMap<>();
            Map<Long, TeamLeaderboardEntryDto> teamInfoMap = new HashMap<>();

            for (RoundTeam team : teams) {
                List<TeamHoleScore> teamScores = teamHoleScoreRepository.findByRoundTeamId(team.getId());
                
                int totalScore = teamScores.stream()
                    .filter(ts -> ts.getGamePoints() != null)
                    .mapToInt(TeamHoleScore::getGamePoints)
                    .sum();

                teamTeeTimeScores.put(team.getId(), totalScore);

                // Build team name and player names
                List<String> playerNames = new ArrayList<>();
                if (team.getPlayer1Id() != null) {
                    playerNames.add(getPlayerName(team.getPlayer1Id()));
                }
                if (team.getPlayer2Id() != null) {
                    playerNames.add(getPlayerName(team.getPlayer2Id()));
                }
                if (team.getPlayer3Id() != null) {
                    playerNames.add(getPlayerName(team.getPlayer3Id()));
                }
                String teamName = String.join(" / ", playerNames);

                teamInfoMap.put(team.getId(), 
                    new TeamLeaderboardEntryDto(team.getId(), teamName, playerNames));
            }

            // Rank teams and assign tee-time points
            assignNinesTeeTimePoints(teamTeeTimeScores, teamInfoMap, leaderboard, round.getId());
        }
    }

    /**
     * Process Stroke round: rank teams by total net score
     */
    private void processStrokeRound(TournamentRound round, Map<Long, TeamLeaderboardEntryDto> leaderboard) {
        System.out.println("\nProcessing stroke round " + round.getId());
        // Get all tee times for this round
        List<RoundTeeTime> teeTimes = roundTeeTimeRepository.findAllByTournamentRoundId(round.getId());
        System.out.println("  Found " + teeTimes.size() + " tee times");

        // Collect all teams and their net scores for this round
        Map<Long, Integer> teamNetScores = new HashMap<>();
        Map<Long, TeamLeaderboardEntryDto> teamInfoMap = new HashMap<>();

        for (RoundTeeTime teeTime : teeTimes) {
            List<RoundTeam> teams = roundTeamRepository.findByRoundTeeTimeId(teeTime.getId());
            System.out.println("    Tee time " + teeTime.getId() + " has " + teams.size() + " teams");

            for (RoundTeam team : teams) {
                // Get all team hole scores for this team in this round
                List<TeamHoleScore> teamScores = teamHoleScoreRepository.findByRoundTeamId(team.getId()).stream()
                    .filter(ts -> ts.getTournamentRound().getId().equals(round.getId()))
                    .collect(Collectors.toList());

                int totalNetScore = teamScores.stream()
                    .map(TeamHoleScore::getNetScore)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();

                System.out.println("      Team " + team.getId() + ": " + teamScores.size() + " hole scores, total net = " + totalNetScore);
                teamNetScores.put(team.getId(), totalNetScore);

                // Build team name if not already in map
                if (!teamInfoMap.containsKey(team.getId())) {
                    List<String> playerNames = new ArrayList<>();
                    if (team.getPlayer1Id() != null) {
                        playerNames.add(getPlayerName(team.getPlayer1Id()));
                    }
                    if (team.getPlayer2Id() != null) {
                        playerNames.add(getPlayerName(team.getPlayer2Id()));
                    }
                    if (team.getPlayer3Id() != null) {
                        playerNames.add(getPlayerName(team.getPlayer3Id()));
                    }
                    String teamName = String.join(" / ", playerNames);

                    teamInfoMap.put(team.getId(), 
                        new TeamLeaderboardEntryDto(team.getId(), teamName, playerNames));
                }
            }
        }

        System.out.println("  Total teams to rank: " + teamNetScores.size());
        // Rank teams and assign stroke round points
        assignStrokeTeeTimePoints(teamNetScores, teamInfoMap, leaderboard, round.getId());
    }

    /**
     * Rank teams within a Nines tee time and assign points
     * Points: 3 for 1st, 2 for 2nd, 1 for 3rd, 0 for others
     * Ties are handled by averaging points
     */
    private void assignNinesTeeTimePoints(
            Map<Long, Integer> teamScores,
            Map<Long, TeamLeaderboardEntryDto> teamInfoMap,
            Map<Long, TeamLeaderboardEntryDto> leaderboard,
            Long roundId) {

        // Sort teams by their score (highest first - Nines scoring)
        List<Map.Entry<Long, Integer>> sortedTeams = teamScores.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue())) // Descending
            .collect(Collectors.toList());

        // Points array: [3, 2, 1] for top 3 finishers
        int[] pointsArray = {3, 2, 1};

        int currentRank = 0;
        int pointsIndex = 0;

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
            double pointsToAward = 0.0;
            if (pointsIndex < pointsArray.length) {
                pointsToAward = pointsArray[pointsIndex];
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
            }

            // Assign points to tied teams
            for (int idx : tiedIndices) {
                Long teamId = sortedTeams.get(idx).getKey();
                
                TeamLeaderboardEntryDto entry = leaderboard.computeIfAbsent(teamId, 
                    tId -> teamInfoMap.get(tId));
                
                entry.addRoundPoints(roundId, pointsToAward);
            }

            currentRank = nextRank;
        }
    }

    /**
     * Rank teams within a stroke round and assign points
     * Points: lowest score (best) gets highest points, highest score (worst) gets 1 point
     * Ties are handled by averaging points
     * numTeams points for 1st place, down to 1 point for last place
     */
    private void assignStrokeTeeTimePoints(
            Map<Long, Integer> teamNetScores,
            Map<Long, TeamLeaderboardEntryDto> teamInfoMap,
            Map<Long, TeamLeaderboardEntryDto> leaderboard,
            Long roundId) {

        if (teamNetScores.isEmpty()) {
            return;
        }

        System.out.println("\n========== Stroke Ranking for Round " + roundId + " ==========");
        System.out.println("Teams and scores: " + teamNetScores);

        // Sort teams by their net score (lowest first - better in stroke play)
        List<Map.Entry<Long, Integer>> sortedTeams = teamNetScores.entrySet().stream()
            .sorted(Map.Entry.comparingByValue()) // Ascending (best score first)
            .collect(Collectors.toList());

        int numTeams = sortedTeams.size();
        System.out.println("Total teams: " + numTeams);
        for (int i = 0; i < sortedTeams.size(); i++) {
            System.out.println("  Team " + sortedTeams.get(i).getKey() + ": score " + sortedTeams.get(i).getValue());
        }

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
            // 1st place (position 0) gets numTeams points
            // Last place (position numTeams-1) gets 1 point
            int pointsForBest = numTeams - currentRank;
            int pointsForWorst = numTeams - (currentRank + tiedIndices.size() - 1);
            double avgPoints = (pointsForBest + pointsForWorst) / 2.0;

            System.out.println("Rank " + (currentRank + 1) + ": " + tiedIndices.size() + " team(s) with score " + 
                currentScore + " - pointsForBest=" + pointsForBest + ", pointsForWorst=" + pointsForWorst + 
                ", avgPoints=" + avgPoints);

            // Assign points to tied teams
            for (int idx : tiedIndices) {
                Long teamId = sortedTeams.get(idx).getKey();
                
                TeamLeaderboardEntryDto entry = leaderboard.computeIfAbsent(teamId, 
                    tId -> teamInfoMap.get(tId));
                
                entry.addRoundPoints(roundId, avgPoints);
                System.out.println("  Assigned " + avgPoints + " points to team " + teamId);
            }

            currentRank = nextRank;
        }
        System.out.println("========== End Stroke Ranking ==========\n");
    }

    private String getPlayerName(Long playerId) {
        Optional<Player> player = playerRepository.findById(playerId);
        if (player.isPresent()) {
            return player.get().getFirstName() + " " + player.get().getLastName();
        }
        return "Unknown";
    }
}
