package com.degen.backend.service;

import com.degen.backend.entity.RoundTeeTime;
import com.degen.backend.entity.TournamentRound;
import com.degen.backend.entity.Player;
import com.degen.backend.dto.PlayerSelectionInfo;
import com.degen.backend.repository.RoundTeeTimeRepository;
import com.degen.backend.repository.TournamentRoundRepository;
import com.degen.backend.repository.TournamentHandicapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeamRandomizationService {

  @Autowired
  private TournamentRoundRepository tournamentRoundRepository;

  @Autowired
  private RoundTeeTimeRepository roundTeeTimeRepository;

  @Autowired
  private TournamentService tournamentService;

  @Autowired
  private TournamentHandicapRepository tournamentHandicapRepository;

  /**
   * Represents a team pairing (combination of 2 players)
   */
  private static class PlayerPair {
    final Long player1;
    final Long player2;

    PlayerPair(Long p1, Long p2) {
      // Normalize so smaller ID is always first
      this.player1 = Math.min(p1, p2);
      this.player2 = Math.max(p1, p2);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof PlayerPair))
        return false;
      PlayerPair other = (PlayerPair) o;
      return this.player1.equals(other.player1) && this.player2.equals(other.player2);
    }

    @Override
    public int hashCode() {
      return Objects.hash(player1, player2);
    }

    @Override
    public String toString() {
      return player1 + "-" + player2;
    }
  }

  /**
   * Represents a team suggestion (list of player IDs)
   */
  public static class TeamSuggestion {
    public List<Long> playerIds;

    public TeamSuggestion(List<Long> playerIds) {
      this.playerIds = playerIds;
    }

    @Override
    public String toString() {
      return playerIds.toString();
    }
  }

  /**
   * Generates randomized team suggestions for a tournament round.
   * Prevents 2-player teams from repeating within the tournament.
   * Attempts (but doesn't strictly enforce) to avoid 3-player team repeats.
   *
   * @param tournamentRoundId The tournament round to randomize
   * @return List of randomized team suggestions
   */
  public List<TeamSuggestion> generateRandomizedTeams(Long tournamentRoundId) {
    // Fetch the tournament round
    Optional<TournamentRound> roundOpt = tournamentRoundRepository.findById(tournamentRoundId);
    if (roundOpt.isEmpty()) {
      throw new RuntimeException("Tournament round not found");
    }

    TournamentRound tournamentRound = roundOpt.get();
    if (tournamentRound.getTournament() == null) {
      throw new RuntimeException("Tournament not found for this round");
    }

    // Get all rounds in this tournament (excluding current round if it has tee
    // times)
    List<TournamentRound> allRounds = tournamentRoundRepository
        .findByTournamentId(tournamentRound.getTournament().getId());

    // Extract all existing team pairings from previous rounds
    Set<PlayerPair> usedPairs = extractUsedPairs(allRounds, tournamentRoundId);
    Set<List<Long>> usedTriples = extractUsedTriples(allRounds, tournamentRoundId);

    // Get all players for this tournament
    List<Player> tournamentPlayers = tournamentService
        .getPlayersByTournamentId(tournamentRound.getTournament().getId());
    List<Long> allPlayerIds = tournamentPlayers.stream()
        .map(Player::getId)
        .collect(Collectors.toList());

    if (allPlayerIds.isEmpty()) {
      throw new RuntimeException("No players found in this tournament");
    }

    // Generate randomized teams
    return generateTeams(allPlayerIds, usedPairs, usedTriples);
  }

  /**
   * Gets player selection info for a tournament including their part-time status
   *
   * @param tournamentRoundId The tournament round ID
   * @return List of PlayerSelectionInfo with part-time status
   */
  public List<PlayerSelectionInfo> getPlayerSelectionInfo(Long tournamentRoundId) {
    // Fetch the tournament round
    Optional<TournamentRound> roundOpt = tournamentRoundRepository.findById(tournamentRoundId);
    if (roundOpt.isEmpty()) {
      throw new RuntimeException("Tournament round not found");
    }

    TournamentRound tournamentRound = roundOpt.get();
    if (tournamentRound.getTournament() == null) {
      throw new RuntimeException("Tournament not found for this round");
    }

    // Get all players and their part-time status
    List<Player> tournamentPlayers = tournamentService
        .getPlayersByTournamentId(tournamentRound.getTournament().getId());

    List<PlayerSelectionInfo> playerInfo = new ArrayList<>();
    for (Player player : tournamentPlayers) {
      Boolean isPartTime = tournamentHandicapRepository
          .findByPlayerIdAndTournamentId(player.getId(), tournamentRound.getTournament().getId())
          .map(th -> th.getPartTime() != null ? th.getPartTime() : false)
          .orElse(false);

      playerInfo.add(new PlayerSelectionInfo(player.getId(), player.getName(), isPartTime));
    }

    // Sort by name for consistent ordering
    playerInfo.sort(Comparator.comparing(PlayerSelectionInfo::getName));

    return playerInfo;
  }

  /**
   * Generates randomized team suggestions based on selected players
   *
   * @param tournamentRoundId The tournament round to randomize
   * @param selectedPlayerIds List of player IDs to randomize
   * @return List of randomized team suggestions
   */
  public List<TeamSuggestion> generateRandomizedTeamsFromSelection(Long tournamentRoundId,
      List<Long> selectedPlayerIds) {
    if (selectedPlayerIds == null || selectedPlayerIds.isEmpty()) {
      throw new RuntimeException("No players selected for randomization");
    }

    // Fetch the tournament round
    Optional<TournamentRound> roundOpt = tournamentRoundRepository.findById(tournamentRoundId);
    if (roundOpt.isEmpty()) {
      throw new RuntimeException("Tournament round not found");
    }

    TournamentRound tournamentRound = roundOpt.get();
    if (tournamentRound.getTournament() == null) {
      throw new RuntimeException("Tournament not found for this round");
    }

    // Get all rounds in this tournament
    List<TournamentRound> allRounds = tournamentRoundRepository
        .findByTournamentId(tournamentRound.getTournament().getId());

    // Extract all existing team pairings from previous rounds
    Set<PlayerPair> usedPairs = extractUsedPairs(allRounds, tournamentRoundId);
    Set<List<Long>> usedTriples = extractUsedTriples(allRounds, tournamentRoundId);

    // Generate randomized teams using only selected players
    return generateTeams(selectedPlayerIds, usedPairs, usedTriples);
  }

  /**
   * Extracts all 2-player pairings used in previous rounds
   */
  private Set<PlayerPair> extractUsedPairs(List<TournamentRound> allRounds, Long currentRoundId) {
    Set<PlayerPair> pairs = new HashSet<>();

    for (TournamentRound round : allRounds) {
      if (round.getId().equals(currentRoundId)) {
        continue; // Skip current round
      }

      List<RoundTeeTime> teeTimes = round.getTeeTimes();
      if (teeTimes == null)
        continue;

      for (RoundTeeTime teeTime : teeTimes) {
        // Check all 2-player combinations in this tee time
        List<Long> playerIds = extractPlayerIds(teeTime);

        // Add all pairwise combinations
        for (int i = 0; i < playerIds.size(); i++) {
          for (int j = i + 1; j < playerIds.size(); j++) {
            pairs.add(new PlayerPair(playerIds.get(i), playerIds.get(j)));
          }
        }
      }
    }

    return pairs;
  }

  /**
   * Extracts all 3-player groupings used in previous rounds
   */
  private Set<List<Long>> extractUsedTriples(List<TournamentRound> allRounds, Long currentRoundId) {
    Set<List<Long>> triples = new HashSet<>();

    for (TournamentRound round : allRounds) {
      if (round.getId().equals(currentRoundId)) {
        continue; // Skip current round
      }

      List<RoundTeeTime> teeTimes = round.getTeeTimes();
      if (teeTimes == null)
        continue;

      for (RoundTeeTime teeTime : teeTimes) {
        List<Long> playerIds = extractPlayerIds(teeTime);

        // Only track 3-player combinations
        if (playerIds.size() >= 3) {
          List<Long> triple = playerIds.subList(0, 3);
          Collections.sort(triple);
          triples.add(triple);
        }
      }
    }

    return triples;
  }

  /**
   * Extracts non-null player IDs from a tee time
   */
  private List<Long> extractPlayerIds(RoundTeeTime teeTime) {
    List<Long> playerIds = new ArrayList<>();
    if (teeTime.getPlayer1Id() != null)
      playerIds.add(teeTime.getPlayer1Id());
    if (teeTime.getPlayer2Id() != null)
      playerIds.add(teeTime.getPlayer2Id());
    if (teeTime.getPlayer3Id() != null)
      playerIds.add(teeTime.getPlayer3Id());
    if (teeTime.getPlayer4Id() != null)
      playerIds.add(teeTime.getPlayer4Id());
    return playerIds;
  }

  /**
   * Generates randomized teams while respecting constraints
   */
  private List<TeamSuggestion> generateTeams(List<Long> allPlayerIds, Set<PlayerPair> usedPairs,
      Set<List<Long>> usedTriples) {
    List<TeamSuggestion> teams = new ArrayList<>();
    Set<Long> usedPlayers = new HashSet<>();
    List<Long> availablePlayers = new ArrayList<>(allPlayerIds);
    Collections.shuffle(availablePlayers);

    // Create 2-player teams - ensure each player is only used once
    for (int i = 0; i < availablePlayers.size(); i++) {
      Long p1 = availablePlayers.get(i);
      if (usedPlayers.contains(p1)) {
        continue;
      }

      // Find a suitable partner for p1
      Long p2 = null;
      for (int j = i + 1; j < availablePlayers.size(); j++) {
        Long candidate = availablePlayers.get(j);
        if (!usedPlayers.contains(candidate)) {
          PlayerPair pair = new PlayerPair(p1, candidate);
          if (!usedPairs.contains(pair)) {
            p2 = candidate;
            break;
          }
        }
      }

      // Create a team with p1 and p2 (if found) or just p1
      if (p2 != null) {
        teams.add(new TeamSuggestion(Arrays.asList(p1, p2)));
        usedPlayers.add(p1);
        usedPlayers.add(p2);
      } else {
        // Can't pair p1 yet, add as single player for now
        teams.add(new TeamSuggestion(Arrays.asList(p1)));
        usedPlayers.add(p1);
      }
    }

    // Post-process: Try to convert single-player teams to 3-player teams
    for (int t = 0; t < teams.size(); t++) {
      TeamSuggestion team = teams.get(t);
      if (team.playerIds.size() == 1) {
        Long singlePlayer = team.playerIds.get(0);
        boolean merged = false;

        // Find a 2-player team to merge with
        for (int s = 0; s < teams.size(); s++) {
          if (s != t) {
            TeamSuggestion other = teams.get(s);
            if (other.playerIds.size() == 2) {
              List<Long> potentialTriple = new ArrayList<>(other.playerIds);
              potentialTriple.add(singlePlayer);
              Collections.sort(potentialTriple);

              if (!usedTriples.contains(potentialTriple)) {
                // Merge the single player into this team
                other.playerIds.add(singlePlayer);
                // Mark this single-player team for removal
                teams.set(t, null);
                merged = true;
                break;
              }
            }
          }
        }
      }
    }

    // Remove null entries (merged teams)
    teams.removeIf(team -> team == null);

    return teams;
  }
}
