package com.degen.backend.controller;

import com.degen.backend.entity.RoundTeeTime;
import com.degen.backend.dto.PlayerSelectionInfo;
import com.degen.backend.service.RoundTeeTimeService;
import com.degen.backend.service.TeamRandomizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/round-tee-times")
@CrossOrigin(origins = "http://localhost:4200")
public class RoundTeeTimeController {

    @Autowired
    private RoundTeeTimeService roundTeeTimeService;

    @Autowired
    private TeamRandomizationService teamRandomizationService;

    @GetMapping("/by-tournament-round/{tournamentRoundId}")
    public Optional<RoundTeeTime> getRoundTeeTimeByTournamentRoundId(@PathVariable Long tournamentRoundId) {
        return roundTeeTimeService.getRoundTeeTimeByTournamentRoundId(tournamentRoundId);
    }

    @GetMapping
    public List<RoundTeeTime> getRoundTeeTimes(@RequestParam(value = "roundId", required = false) Long roundId) {
        if (roundId != null) {
            return roundTeeTimeService.getRoundTeeTimesByRoundId(roundId);
        }
        return roundTeeTimeService.getAllRoundTeeTimes();
    }

    @GetMapping("/{id}")
    public Optional<RoundTeeTime> getRoundTeeTimeById(@PathVariable Long id) {
        return roundTeeTimeService.getRoundTeeTimeById(id);
    }

    @PostMapping
    public RoundTeeTime createRoundTeeTime(@RequestBody RoundTeeTime roundTeeTime) {
        return roundTeeTimeService.saveRoundTeeTime(roundTeeTime);
    }

    @PutMapping("/{id}")
    public RoundTeeTime updateRoundTeeTime(@PathVariable Long id, @RequestBody RoundTeeTime roundTeeTime) {
        roundTeeTime.setId(id);
        return roundTeeTimeService.saveRoundTeeTime(roundTeeTime);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoundTeeTime(@PathVariable Long id) {
        try {
            roundTeeTimeService.deleteRoundTeeTime(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("scorecards")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Cannot delete tee time: scorecards exist for this tee time");
                error.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }
            throw e;
        }
    }

    @GetMapping("/randomize/{tournamentRoundId}")
    public ResponseEntity<List<TeamRandomizationService.TeamSuggestion>> randomizeTeams(
            @PathVariable Long tournamentRoundId) {
        try {
            List<TeamRandomizationService.TeamSuggestion> suggestions = teamRandomizationService
                    .generateRandomizedTeams(tournamentRoundId);
            return ResponseEntity.ok(suggestions);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/player-selection/{tournamentRoundId}")
    public ResponseEntity<List<PlayerSelectionInfo>> getPlayerSelectionInfo(
            @PathVariable Long tournamentRoundId) {
        try {
            List<PlayerSelectionInfo> playerInfo = teamRandomizationService
                    .getPlayerSelectionInfo(tournamentRoundId);
            return ResponseEntity.ok(playerInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/randomize-selection/{tournamentRoundId}")
    public ResponseEntity<?> randomizeTeamsFromSelection(
            @PathVariable Long tournamentRoundId,
            @RequestBody List<Long> selectedPlayerIds) {
        try {
            System.out.println("Randomizing teams for round " + tournamentRoundId + " with "
                    + (selectedPlayerIds != null ? selectedPlayerIds.size() : 0) + " players");
            List<TeamRandomizationService.TeamSuggestion> suggestions = teamRandomizationService
                    .generateRandomizedTeamsFromSelection(tournamentRoundId, selectedPlayerIds);
            return ResponseEntity.ok(suggestions);
        } catch (RuntimeException e) {
            System.err.println("Error randomizing teams: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
