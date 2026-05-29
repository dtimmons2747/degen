package com.degen.backend.service;

import com.degen.backend.entity.Player;
import com.degen.backend.entity.Tournament;
import com.degen.backend.repository.TournamentHandicapRepository;
import com.degen.backend.repository.TournamentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TournamentService {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentHandicapRepository tournamentHandicapRepository;

    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }

    public Optional<Tournament> getTournamentById(Long id) {
        return tournamentRepository.findById(id);
    }

    public Tournament saveTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    public void deleteTournament(Long id) {
        tournamentRepository.deleteById(id);
    }

    public List<Player> getPlayersByTournamentId(Long tournamentId) {
        return tournamentHandicapRepository.findPlayersByTournamentId(tournamentId);
    }
}