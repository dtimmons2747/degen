package com.degen.backend.service;

import com.degen.backend.entity.TournamentRound;
import com.degen.backend.repository.TournamentRoundRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TournamentRoundService {

    @Autowired
    private TournamentRoundRepository tournamentRoundRepository;

    public List<TournamentRound> getAllTournamentRounds() {
        return tournamentRoundRepository.findAll();
    }

    public Optional<TournamentRound> getTournamentRoundById(Long id) {
        return tournamentRoundRepository.findById(id);
    }

    public List<TournamentRound> getTournamentRoundsByTournamentId(Long tournamentId) {
        return tournamentRoundRepository.findByTournamentId(tournamentId);
    }

    public TournamentRound saveTournamentRound(TournamentRound tournamentRound) {
        return tournamentRoundRepository.save(tournamentRound);
    }

    public TournamentRound patchTournamentRound(Long id, TournamentRound updates) {
        Optional<TournamentRound> existing = tournamentRoundRepository.findById(id);
        if (existing.isPresent()) {
            TournamentRound round = existing.get();
            if (updates.getGame() != null) {
                round.setGame(updates.getGame());
            }
            if (updates.getCourse() != null) {
                round.setCourse(updates.getCourse());
            }
            if (updates.getScoringType() != null) {
                round.setScoringType(updates.getScoringType());
            }
            if (updates.getSplitSkins() != null) {
                round.setSplitSkins(updates.getSplitSkins());
            }
            if (updates.getVsGroup() != null) {
                round.setVsGroup(updates.getVsGroup());
            }
            return tournamentRoundRepository.save(round);
        }
        throw new RuntimeException("TournamentRound not found with id: " + id);
    }

    public void deleteTournamentRound(Long id) {
        tournamentRoundRepository.deleteById(id);
    }
}
