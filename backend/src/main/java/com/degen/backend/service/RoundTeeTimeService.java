package com.degen.backend.service;

import com.degen.backend.entity.RoundTeeTime;
import com.degen.backend.entity.TournamentRound;
import com.degen.backend.entity.RoundTeam;
import com.degen.backend.entity.RoundHandicap;
import com.degen.backend.repository.RoundTeeTimeRepository;
import com.degen.backend.repository.TournamentRoundRepository;
import com.degen.backend.repository.RoundTeamRepository;
import com.degen.backend.repository.RoundHandicapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RoundTeeTimeService {

    @Autowired
    private RoundTeeTimeRepository roundTeeTimeRepository;

    @Autowired
    private TournamentRoundRepository tournamentRoundRepository;

    @Autowired
    private RoundTeamRepository roundTeamRepository;

    @Autowired
    private RoundHandicapRepository roundHandicapRepository;

    @Autowired
    private PlayerScorecardService playerScorecardService;

    public List<RoundTeeTime> getAllRoundTeeTimes() {
        try {
            List<RoundTeeTime> teeTimes = roundTeeTimeRepository.findAll();
            teeTimes.forEach(this::enrichWithHandicaps);
            return teeTimes;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching all tee times: " + e.getMessage(), e);
        }
    }

    public Optional<RoundTeeTime> getRoundTeeTimeById(Long id) {
        try {
            Optional<RoundTeeTime> teeTime = roundTeeTimeRepository.findById(id);
            teeTime.ifPresent(this::enrichWithHandicaps);
            return teeTime;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching tee time by id: " + e.getMessage(), e);
        }
    }

    public Optional<RoundTeeTime> getRoundTeeTimeByTournamentRoundId(Long tournamentRoundId) {
        try {
            Optional<RoundTeeTime> teeTime = roundTeeTimeRepository.findByTournamentRoundId(tournamentRoundId);
            teeTime.ifPresent(this::enrichWithHandicaps);
            return teeTime;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching tee time by tournament round: " + e.getMessage(), e);
        }
    }

    public List<RoundTeeTime> getRoundTeeTimesByRoundId(Long roundId) {
        try {
            List<RoundTeeTime> teeTimes = roundTeeTimeRepository.findAllByTournamentRoundId(roundId);
            teeTimes.forEach(this::enrichWithHandicaps);
            return teeTimes;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching tee times by round id: " + e.getMessage(), e);
        }
    }

    public RoundTeeTime saveRoundTeeTime(RoundTeeTime roundTeeTime) {
        try {
            // Handle tournamentRoundId transient field - convert to actual TournamentRound
            // reference
            if (roundTeeTime.getTournamentRoundId() != null && roundTeeTime.getTournamentRound() == null) {
                TournamentRound tr = new TournamentRound();
                tr.setId(roundTeeTime.getTournamentRoundId());
                roundTeeTime.setTournamentRound(tr);
            }

            // If this is an update (ID exists), fetch the existing entity and merge changes
            if (roundTeeTime.getId() != null) {
                Optional<RoundTeeTime> existing = roundTeeTimeRepository.findById(roundTeeTime.getId());
                if (existing.isPresent()) {
                    RoundTeeTime existingTeeTime = existing.get();

                    // Track if player assignments changed
                    boolean playerAssignmentChanged = false;
                    if (!objectsEqual(existingTeeTime.getPlayer1Id(), roundTeeTime.getPlayer1Id()) ||
                            !objectsEqual(existingTeeTime.getPlayer2Id(), roundTeeTime.getPlayer2Id()) ||
                            !objectsEqual(existingTeeTime.getPlayer3Id(), roundTeeTime.getPlayer3Id()) ||
                            !objectsEqual(existingTeeTime.getPlayer4Id(), roundTeeTime.getPlayer4Id())) {
                        playerAssignmentChanged = true;
                    }

                    // Update only the provided fields
                    if (roundTeeTime.getTeeTime() != null) {
                        existingTeeTime.setTeeTime(roundTeeTime.getTeeTime());
                    }
                    // Update player IDs (allow null/0 to clear the slot)
                    existingTeeTime.setPlayer1Id(roundTeeTime.getPlayer1Id());
                    existingTeeTime.setPlayer2Id(roundTeeTime.getPlayer2Id());
                    existingTeeTime.setPlayer3Id(roundTeeTime.getPlayer3Id());
                    existingTeeTime.setPlayer4Id(roundTeeTime.getPlayer4Id());

                    // tournamentRound is preserved from the existing entity
                    RoundTeeTime saved = roundTeeTimeRepository.save(existingTeeTime);

                    // Save round handicaps from transient fields (returns set of players whose
                    // handicaps changed)
                    java.util.Set<Long> changedPlayerIds = saveRoundHandicaps(saved, roundTeeTime);

                    // If player assignments changed, recreate teams
                    // Otherwise, recalculate scores only for players with changed handicaps
                    if (playerAssignmentChanged) {
                        createTeamsForTeeTime(saved);
                    } else if (!changedPlayerIds.isEmpty()) {
                        // Only handicaps changed - recalculate net scores for affected players
                        playerScorecardService.recalculateScoresForTeeTime(saved.getId(), changedPlayerIds);
                    }

                    return saved;
                }
            }

            // For new records, ensure tournamentRound is not null with an ID
            if (roundTeeTime.getTournamentRound() == null || roundTeeTime.getTournamentRound().getId() == null) {
                throw new IllegalArgumentException("TournamentRound with ID is required for new tee times");
            }

            // Save the new tee time - Hibernate will use the stub TournamentRound with just
            // the ID
            RoundTeeTime saved = roundTeeTimeRepository.save(roundTeeTime);

            // Save round handicaps from transient fields
            saveRoundHandicaps(saved, roundTeeTime);

            // Create teams if this is an inter-group round
            createTeamsForTeeTime(saved);

            return saved;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error saving tee time: " + e.getMessage(), e);
        }
    }

    private boolean objectsEqual(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        return obj1.equals(obj2);
    }

    public void deleteRoundTeeTime(Long id) {
        try {
            // Check if there are any scorecards for this tee time
            if (playerScorecardService.hasScorecards(id)) {
                throw new RuntimeException("Cannot delete tee time: scorecards exist for this tee time");
            }
            // Delete associated round handicaps
            roundHandicapRepository.deleteByRoundTeeTimeId(id);
            // Delete the tee time
            roundTeeTimeRepository.deleteById(id);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error deleting tee time: " + e.getMessage(), e);
        }
    }

    private java.util.Set<Long> saveRoundHandicaps(RoundTeeTime roundTeeTime, RoundTeeTime sourceData) {
        java.util.Set<Long> changedPlayerIds = new java.util.HashSet<>();
        try {
            // Map of player position to handicap value and player ID
            Long[][] playerData = {
                    { roundTeeTime.getPlayer1Id(), sourceData.getPlayer1Id() },
                    { roundTeeTime.getPlayer2Id(), sourceData.getPlayer2Id() },
                    { roundTeeTime.getPlayer3Id(), sourceData.getPlayer3Id() },
                    { roundTeeTime.getPlayer4Id(), sourceData.getPlayer4Id() }
            };
            Double[] handicaps = {
                    sourceData.getPlayer1Handicap(),
                    sourceData.getPlayer2Handicap(),
                    sourceData.getPlayer3Handicap(),
                    sourceData.getPlayer4Handicap()
            };

            for (int i = 0; i < playerData.length; i++) {
                Long playerId = playerData[i][0];
                Double handicap = handicaps[i];

                if (playerId != null && playerId > 0) {
                    Optional<RoundHandicap> existing = roundHandicapRepository
                            .findByRoundTeeTimeIdAndPlayerId(roundTeeTime.getId(), playerId);

                    if (handicap != null && handicap > 0) {
                        // Check if handicap value actually changed
                        Double existingHandicapValue = existing.isPresent() ? existing.get().getHandicap() : null;
                        if (existingHandicapValue == null || !existingHandicapValue.equals(handicap)) {
                            changedPlayerIds.add(playerId);
                        }

                        // Create or update round handicap
                        RoundHandicap roundHandicap = existing.orElseGet(RoundHandicap::new);
                        roundHandicap.setRoundTeeTime(roundTeeTime);
                        roundHandicap.setPlayerId(playerId);
                        roundHandicap.setHandicap(handicap);
                        roundHandicapRepository.save(roundHandicap);
                    } else if (existing.isPresent()) {
                        // Handicap removed - mark player as changed
                        changedPlayerIds.add(playerId);
                        roundHandicapRepository.delete(existing.get());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error saving round handicaps: " + e.getMessage(), e);
        }
        return changedPlayerIds;
    }

    private void enrichWithHandicaps(RoundTeeTime roundTeeTime) {
        try {
            if (roundTeeTime == null || roundTeeTime.getId() == null) {
                return;
            }

            List<RoundHandicap> handicaps = roundHandicapRepository.findByRoundTeeTimeId(roundTeeTime.getId());

            System.out.println("DEBUG: enrichWithHandicaps for roundTeeTime " + roundTeeTime.getId() +
                    " - Found " + handicaps.size() + " round handicap records");
            System.out.println("DEBUG: Player IDs in teeTime: player1=" + roundTeeTime.getPlayer1Id() +
                    ", player2=" + roundTeeTime.getPlayer2Id() +
                    ", player3=" + roundTeeTime.getPlayer3Id() +
                    ", player4=" + roundTeeTime.getPlayer4Id());

            for (RoundHandicap rh : handicaps) {
                System.out.println("DEBUG: Processing round handicap - playerId=" + rh.getPlayerId() + ", handicap="
                        + rh.getHandicap());

                if (rh.getPlayerId() != null && rh.getHandicap() != null) {
                    if (rh.getPlayerId().equals(roundTeeTime.getPlayer1Id())) {
                        roundTeeTime.setPlayer1Handicap(rh.getHandicap());
                        System.out.println("DEBUG: Set Player1 handicap to " + rh.getHandicap());
                    } else if (rh.getPlayerId().equals(roundTeeTime.getPlayer2Id())) {
                        roundTeeTime.setPlayer2Handicap(rh.getHandicap());
                        System.out.println("DEBUG: Set Player2 handicap to " + rh.getHandicap());
                    } else if (rh.getPlayerId().equals(roundTeeTime.getPlayer3Id())) {
                        roundTeeTime.setPlayer3Handicap(rh.getHandicap());
                        System.out.println("DEBUG: Set Player3 handicap to " + rh.getHandicap());
                    } else if (rh.getPlayerId().equals(roundTeeTime.getPlayer4Id())) {
                        roundTeeTime.setPlayer4Handicap(rh.getHandicap());
                        System.out.println("DEBUG: Set Player4 handicap to " + rh.getHandicap());
                    } else {
                        System.out.println(
                                "DEBUG: PlayerId " + rh.getPlayerId() + " doesn't match any player in this teeTime");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Don't throw - just log the error so it doesn't break the main response
        }
    }

    @Transactional
    private void createTeamsForTeeTime(RoundTeeTime roundTeeTime) {
        try {
            // Always create teams for inter-group scoring
            if (roundTeeTime.getTournamentRound() == null) {
                return; // No tournament round, skip team creation
            }

            // Skip team creation for Individual games
            if (roundTeeTime.getTournamentRound().getGame() != null &&
                    roundTeeTime.getTournamentRound().getGame().getId() == 4L) {
                System.out.println("Skipping team creation for Individual game");
                return; // Individual games don't have teams
            }

            // Delete existing teams for this tee time
            roundTeamRepository.deleteAll(roundTeamRepository.findByRoundTeeTimeId(roundTeeTime.getId()));

            // Collect all player IDs for this tee time
            List<Long> playerIds = new java.util.ArrayList<>();
            if (roundTeeTime.getPlayer1Id() != null && roundTeeTime.getPlayer1Id() > 0) {
                playerIds.add(roundTeeTime.getPlayer1Id());
            }
            if (roundTeeTime.getPlayer2Id() != null && roundTeeTime.getPlayer2Id() > 0) {
                playerIds.add(roundTeeTime.getPlayer2Id());
            }
            if (roundTeeTime.getPlayer3Id() != null && roundTeeTime.getPlayer3Id() > 0) {
                playerIds.add(roundTeeTime.getPlayer3Id());
            }
            if (roundTeeTime.getPlayer4Id() != null && roundTeeTime.getPlayer4Id() > 0) {
                playerIds.add(roundTeeTime.getPlayer4Id());
            }

            if (playerIds.isEmpty()) {
                return; // No players assigned, skip team creation
            }

            System.out.println("Creating teams for tee time with " + playerIds.size() + " players");

            // Create teams based on number of players
            if (playerIds.size() == 2) {
                // 2 players: 1 team
                RoundTeam team = new RoundTeam();
                team.setRoundTeeTime(roundTeeTime);
                team.setPlayer1Id(playerIds.get(0));
                team.setPlayer2Id(playerIds.get(1));
                roundTeamRepository.save(team);
                System.out.println("Created 1 team with 2 players");
            } else if (playerIds.size() == 3) {
                // 3 players: 1 team
                RoundTeam team = new RoundTeam();
                team.setRoundTeeTime(roundTeeTime);
                team.setPlayer1Id(playerIds.get(0));
                team.setPlayer2Id(playerIds.get(1));
                team.setPlayer3Id(playerIds.get(2));
                roundTeamRepository.save(team);
                System.out.println("Created 1 team with 3 players");
            } else if (playerIds.size() == 4) {
                // 4 players: 2 teams (players 1&2 in team 1, players 3&4 in team 2)
                RoundTeam team1 = new RoundTeam();
                team1.setRoundTeeTime(roundTeeTime);
                team1.setPlayer1Id(playerIds.get(0));
                team1.setPlayer2Id(playerIds.get(1));
                roundTeamRepository.save(team1);

                RoundTeam team2 = new RoundTeam();
                team2.setRoundTeeTime(roundTeeTime);
                team2.setPlayer1Id(playerIds.get(2));
                team2.setPlayer2Id(playerIds.get(3));
                roundTeamRepository.save(team2);
                System.out.println("Created 2 teams with 2 players each");
            } else if (playerIds.size() == 6) {
                // 6 players: 2 teams of 3
                RoundTeam team1 = new RoundTeam();
                team1.setRoundTeeTime(roundTeeTime);
                team1.setPlayer1Id(playerIds.get(0));
                team1.setPlayer2Id(playerIds.get(1));
                team1.setPlayer3Id(playerIds.get(2));
                roundTeamRepository.save(team1);

                RoundTeam team2 = new RoundTeam();
                team2.setRoundTeeTime(roundTeeTime);
                team2.setPlayer1Id(playerIds.get(3));
                team2.setPlayer2Id(playerIds.get(4));
                team2.setPlayer3Id(playerIds.get(5));
                roundTeamRepository.save(team2);
                System.out.println("Created 2 teams with 3 players each");
            } else if (playerIds.size() == 9) {
                // 9 players: 3 teams of 3 (Nines format)
                RoundTeam team1 = new RoundTeam();
                team1.setRoundTeeTime(roundTeeTime);
                team1.setPlayer1Id(playerIds.get(0));
                team1.setPlayer2Id(playerIds.get(1));
                team1.setPlayer3Id(playerIds.get(2));
                roundTeamRepository.save(team1);

                RoundTeam team2 = new RoundTeam();
                team2.setRoundTeeTime(roundTeeTime);
                team2.setPlayer1Id(playerIds.get(3));
                team2.setPlayer2Id(playerIds.get(4));
                team2.setPlayer3Id(playerIds.get(5));
                roundTeamRepository.save(team2);

                RoundTeam team3 = new RoundTeam();
                team3.setRoundTeeTime(roundTeeTime);
                team3.setPlayer1Id(playerIds.get(6));
                team3.setPlayer2Id(playerIds.get(7));
                team3.setPlayer3Id(playerIds.get(8));
                roundTeamRepository.save(team3);
                System.out.println("Created 3 teams with 3 players each (Nines format)");
            } else {
                System.out.println("Unsupported number of players: " + playerIds.size() + ". Teams not created.");
            }
        } catch (Exception e) {
            System.out.println("Error creating teams for tee time: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
