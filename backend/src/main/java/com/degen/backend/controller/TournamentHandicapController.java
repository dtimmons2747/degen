package com.degen.backend.controller;

import com.degen.backend.entity.TournamentHandicap;
import com.degen.backend.service.TournamentHandicapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tournament-handicaps")
@CrossOrigin(origins = "http://localhost:4200")
public class TournamentHandicapController {

    @Autowired
    private TournamentHandicapService tournamentHandicapService;

    @GetMapping
    public List<TournamentHandicap> getAllTournamentHandicaps(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long playerId) {
        if (tournamentId != null && playerId != null) {
            return tournamentHandicapService.getTournamentHandicapsByTournamentIdAndPlayerId(tournamentId, playerId);
        }
        if (tournamentId != null) {
            return tournamentHandicapService.getTournamentHandicapsByTournamentId(tournamentId);
        }
        return tournamentHandicapService.getAllTournamentHandicaps();
    }

    @GetMapping("/{id}")
    public Optional<TournamentHandicap> getTournamentHandicapById(@PathVariable Long id) {
        return tournamentHandicapService.getTournamentHandicapById(id);
    }

    @PostMapping
    public TournamentHandicap createTournamentHandicap(@RequestBody TournamentHandicap tournamentHandicap) {
        return tournamentHandicapService.saveTournamentHandicap(tournamentHandicap);
    }

    @PutMapping("/{id}")
    public TournamentHandicap updateTournamentHandicap(@PathVariable Long id, @RequestBody TournamentHandicap tournamentHandicap) {
        tournamentHandicap.setId(id);
        return tournamentHandicapService.saveTournamentHandicap(tournamentHandicap);
    }

    @DeleteMapping("/{id}")
    public void deleteTournamentHandicap(@PathVariable Long id) {
        tournamentHandicapService.deleteTournamentHandicap(id);
    }
}