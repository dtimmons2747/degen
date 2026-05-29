package com.degen.backend.controller;

import com.degen.backend.entity.TournamentRound;
import com.degen.backend.service.TournamentRoundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tournament-rounds")
@CrossOrigin(origins = "http://localhost:4200")
public class TournamentRoundController {

    @Autowired
    private TournamentRoundService tournamentRoundService;

    @GetMapping
    public List<TournamentRound> getTournamentRounds(@RequestParam(required = false) Long tournamentId) {
        if (tournamentId != null) {
            return tournamentRoundService.getTournamentRoundsByTournamentId(tournamentId);
        }
        return tournamentRoundService.getAllTournamentRounds();
    }

    @GetMapping("/{id}")
    public Optional<TournamentRound> getTournamentRoundById(@PathVariable Long id) {
        return tournamentRoundService.getTournamentRoundById(id);
    }

    @PostMapping
    public TournamentRound createTournamentRound(@RequestBody TournamentRound tournamentRound) {
        return tournamentRoundService.saveTournamentRound(tournamentRound);
    }

    @PutMapping("/{id}")
    public TournamentRound updateTournamentRound(@PathVariable Long id, @RequestBody TournamentRound tournamentRound) {
        tournamentRound.setId(id);
        return tournamentRoundService.saveTournamentRound(tournamentRound);
    }

    @PatchMapping("/{id}")
    public TournamentRound patchTournamentRound(@PathVariable Long id, @RequestBody TournamentRound tournamentRound) {
        return tournamentRoundService.patchTournamentRound(id, tournamentRound);
    }

    @DeleteMapping("/{id}")
    public void deleteTournamentRound(@PathVariable Long id) {
        tournamentRoundService.deleteTournamentRound(id);
    }
}

