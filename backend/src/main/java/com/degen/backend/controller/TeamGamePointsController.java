package com.degen.backend.controller;

import com.degen.backend.entity.RoundTeam;
import com.degen.backend.entity.TeamHoleScore;
import com.degen.backend.repository.RoundTeamRepository;
import com.degen.backend.repository.TeamHoleScoreRepository;
import com.degen.backend.service.TeamGamePointsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/team-game-points")
public class TeamGamePointsController {

    @Autowired
    private TeamGamePointsService teamGamePointsService;

    @Autowired
    private RoundTeamRepository roundTeamRepository;

    @Autowired
    private TeamHoleScoreRepository teamHoleScoreRepository;

    /**
     * Generate team hole scores for a specific tee time
     * Calculates best net score for each team for each hole
     */
    @PostMapping("/generate-team-scores/{roundTeeTimeId}")
    public ResponseEntity<Map<String, String>> generateTeamHoleScores(@PathVariable Long roundTeeTimeId) {
        try {
            teamGamePointsService.generateTeamHoleScores(roundTeeTimeId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Team hole scores generated successfully");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error: " + e.getMessage());
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Calculate and save game points for all teams in a tournament round
     * Compares team scores across all tee times
     */
    @PostMapping("/calculate-points/{tournamentRoundId}")
    public ResponseEntity<Map<String, String>> calculateTeamGamePoints(@PathVariable Long tournamentRoundId) {
        try {
            teamGamePointsService.calculateAndSaveTeamGamePoints(tournamentRoundId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Team game points calculated successfully");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            // Validation error - incomplete scorecards
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "validation_error");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error: " + e.getMessage());
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get all teams for a specific tee time
     */
    @GetMapping("/tee-time/{roundTeeTimeId}")
    public ResponseEntity<List<RoundTeam>> getTeamsForTeeTime(@PathVariable Long roundTeeTimeId) {
        List<RoundTeam> teams = roundTeamRepository.findByRoundTeeTimeId(roundTeeTimeId);
        return ResponseEntity.ok(teams);
    }

    /**
     * Get team hole scores for a specific team
     */
    @GetMapping("/scores/{roundTeamId}")
    public ResponseEntity<List<TeamHoleScore>> getTeamHoleScores(@PathVariable Long roundTeamId) {
        List<TeamHoleScore> scores = teamHoleScoreRepository.findByRoundTeamId(roundTeamId);
        return ResponseEntity.ok(scores);
    }

    /**
     * Get stroke-based team rankings for a tournament round
     * Used for 2-Man Aggregate and other stroke scoring games
     */
    @GetMapping("/stroke-rankings/{tournamentRoundId}")
    public ResponseEntity<Map<String, Object>> getStrokeRankings(@PathVariable Long tournamentRoundId) {
        try {
            Map<Long, Double> rankings = teamGamePointsService.calculateStrokeRankings(tournamentRoundId);
            Map<String, Object> response = new HashMap<>();
            response.put("rankings", rankings);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Save team game points for a specific team and hole
     * Used for Stableford scoring where points are calculated as scores are entered
     */
    @Transactional
    @PutMapping("/save-game-points/{roundTeamId}/{holeId}")
    public ResponseEntity<Map<String, String>> saveTeamGamePoints(
            @PathVariable Long roundTeamId,
            @PathVariable Long holeId,
            @RequestBody Map<String, Integer> request) {
        try {
            Integer gamePoints = request.get("gamePoints");
            if (gamePoints == null) {
                throw new IllegalArgumentException("gamePoints is required");
            }

            // Find the team hole score for this team and hole
            RoundTeam roundTeam = roundTeamRepository.findById(roundTeamId)
                    .orElseThrow(() -> new IllegalArgumentException("Round team not found with id: " + roundTeamId));

            TeamHoleScore teamHoleScore = teamHoleScoreRepository.findByRoundTeamIdAndHoleId(roundTeamId, holeId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Team hole score not found for team " + roundTeamId + " and hole " + holeId));

            teamHoleScore.setGamePoints(gamePoints);
            teamHoleScoreRepository.save(teamHoleScore);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Team game points saved successfully");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error: " + e.getMessage());
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }
}
