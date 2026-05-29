package com.degen.backend.service;

import com.degen.backend.entity.TournamentHandicap;
import com.degen.backend.entity.RoundHandicap;
import com.degen.backend.entity.RoundTeeTime;
import com.degen.backend.entity.TournamentRound;
import com.degen.backend.entity.PlayerScorecard;
import com.degen.backend.repository.TournamentHandicapRepository;
import com.degen.backend.repository.RoundHandicapRepository;
import com.degen.backend.repository.RoundTeeTimeRepository;
import com.degen.backend.repository.TournamentRoundRepository;
import com.degen.backend.repository.PlayerScorecardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TournamentHandicapService {

    @Autowired
    private TournamentHandicapRepository tournamentHandicapRepository;

    @Autowired
    private RoundHandicapRepository roundHandicapRepository;

    @Autowired
    private RoundTeeTimeRepository roundTeeTimeRepository;

    @Autowired
    private TournamentRoundRepository tournamentRoundRepository;

    @Autowired
    private PlayerScorecardRepository playerScorecardRepository;

    public List<TournamentHandicap> getAllTournamentHandicaps() {
        return tournamentHandicapRepository.findAll();
    }

    public Optional<TournamentHandicap> getTournamentHandicapById(Long id) {
        return tournamentHandicapRepository.findById(id);
    }

    public List<TournamentHandicap> getTournamentHandicapsByTournamentId(Long tournamentId) {
        return tournamentHandicapRepository.findByTournamentId(tournamentId);
    }

    public List<TournamentHandicap> getTournamentHandicapsByTournamentIdAndPlayerId(Long tournamentId, Long playerId) {
        return tournamentHandicapRepository.findByTournamentIdAndPlayerId(tournamentId, playerId);
    }

    public TournamentHandicap saveTournamentHandicap(TournamentHandicap tournamentHandicap) {
        try {
            // If this is an update (ID exists), fetch the existing entity and merge changes
            if (tournamentHandicap.getId() != null) {
                Optional<TournamentHandicap> existing = tournamentHandicapRepository
                        .findById(tournamentHandicap.getId());
                if (existing.isPresent()) {
                    TournamentHandicap existingHandicap = existing.get();

                    // Store the player and tournament before any updates
                    final Long playerId = existingHandicap.getPlayer() != null ? existingHandicap.getPlayer().getId()
                            : null;
                    final Long tournamentId = existingHandicap.getTournament() != null
                            ? existingHandicap.getTournament().getId()
                            : null;

                    // Track if handicap was updated
                    Double newHandicap = null;

                    // Update only the provided fields
                    if (tournamentHandicap.getHandicap() != null) {
                        existingHandicap.setHandicap(tournamentHandicap.getHandicap());
                        newHandicap = tournamentHandicap.getHandicap();
                    }
                    if (tournamentHandicap.getPartTime() != null) {
                        existingHandicap.setPartTime(tournamentHandicap.getPartTime());
                    }

                    // Save the updated entity
                    TournamentHandicap saved = tournamentHandicapRepository.save(existingHandicap);

                    // Verify that player and tournament were not erased
                    if (saved.getPlayer() == null || saved.getTournament() == null) {
                        System.err.println("WARNING: Player or Tournament was set to null during save!");
                        System.err.println("PlayerId: " + playerId + ", TournamentId: " + tournamentId);
                    }

                    // If handicap was updated, cascade to round handicaps (only for rounds with no
                    // scores)
                    if (newHandicap != null && playerId != null && tournamentId != null) {
                        updateRoundHandicapsForPlayerIfNoScores(playerId, tournamentId, newHandicap);
                    }

                    return saved;
                }
            }

            // For new records, ensure player and tournament are not null
            if (tournamentHandicap.getPlayer() == null || tournamentHandicap.getTournament() == null) {
                throw new IllegalArgumentException("Player and Tournament are required for new tournament handicaps");
            }
            return tournamentHandicapRepository.save(tournamentHandicap);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error saving tournament handicap: " + e.getMessage(), e);
        }
    }

    public void deleteTournamentHandicap(Long id) {
        tournamentHandicapRepository.deleteById(id);
    }

    /**
     * Updates RoundHandicap records for a player in a tournament.
     * Only updates rounds where the player has no scores entered.
     * 
     * @param playerId     The player to update
     * @param tournamentId The tournament to search in
     * @param newHandicap  The new handicap value to set
     */
    private void updateRoundHandicapsForPlayerIfNoScores(Long playerId, Long tournamentId, Double newHandicap) {
        try {
            // Get all tournament rounds for this tournament
            List<TournamentRound> tournamentRounds = tournamentRoundRepository.findByTournamentId(tournamentId);

            for (TournamentRound round : tournamentRounds) {
                // Get all tee times for this round
                List<RoundTeeTime> teeTimes = roundTeeTimeRepository.findAllByTournamentRoundId(round.getId());

                for (RoundTeeTime teeTime : teeTimes) {
                    // Check if this player is in this tee time (any of the 4 slots)
                    boolean playerInTeeTime = playerId.equals(teeTime.getPlayer1Id()) ||
                            playerId.equals(teeTime.getPlayer2Id()) ||
                            playerId.equals(teeTime.getPlayer3Id()) ||
                            playerId.equals(teeTime.getPlayer4Id());

                    if (playerInTeeTime) {
                        // Check if player has any scores for this tee time
                        List<PlayerScorecard> playerScores = playerScorecardRepository.findByRoundTeeTimeIdAndPlayerId(
                                teeTime.getId(),
                                playerId);

                        // Only update if there are no scores
                        if (playerScores == null || playerScores.isEmpty()) {
                            // Find and update the RoundHandicap for this player in this tee time
                            Optional<RoundHandicap> roundHandicap = roundHandicapRepository
                                    .findByRoundTeeTimeIdAndPlayerId(
                                            teeTime.getId(),
                                            playerId);

                            if (roundHandicap.isPresent()) {
                                RoundHandicap rh = roundHandicap.get();
                                rh.setHandicap(newHandicap);
                                roundHandicapRepository.save(rh);
                                System.out.println("Updated RoundHandicap for player " + playerId +
                                        " in tee time " + teeTime.getId() +
                                        " with new handicap: " + newHandicap);
                            }
                        } else {
                            System.out.println("Skipped updating RoundHandicap for player " + playerId +
                                    " in tee time " + teeTime.getId() +
                                    " because scores exist for this round");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating round handicaps: " + e.getMessage());
            e.printStackTrace();
            // Don't throw exception - this is a secondary operation that shouldn't block
            // the main save
        }
    }
}