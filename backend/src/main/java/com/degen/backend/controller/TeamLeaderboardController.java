package com.degen.backend.controller;

import com.degen.backend.dto.TeamLeaderboardEntryDto;
import com.degen.backend.service.TeamLeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team-leaderboard")
public class TeamLeaderboardController {

    private final TeamLeaderboardService teamLeaderboardService;

    public TeamLeaderboardController(TeamLeaderboardService teamLeaderboardService) {
        this.teamLeaderboardService = teamLeaderboardService;
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<TeamLeaderboardEntryDto>> getTournamentTeamLeaderboard(
            @PathVariable Long tournamentId) {
        List<TeamLeaderboardEntryDto> leaderboard = teamLeaderboardService.getTournamentTeamLeaderboard(tournamentId);
        return ResponseEntity.ok(leaderboard);
    }
}
