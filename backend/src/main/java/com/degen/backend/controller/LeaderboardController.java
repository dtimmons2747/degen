package com.degen.backend.controller;

import com.degen.backend.dto.LeaderboardEntryDto;
import com.degen.backend.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<LeaderboardEntryDto>> getTournamentLeaderboard(
            @PathVariable Long tournamentId) {
        List<LeaderboardEntryDto> leaderboard = leaderboardService.getTournamentLeaderboard(tournamentId);
        return ResponseEntity.ok(leaderboard);
    }
}
