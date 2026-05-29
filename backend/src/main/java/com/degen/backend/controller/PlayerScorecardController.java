package com.degen.backend.controller;

import com.degen.backend.entity.PlayerScorecard;
import com.degen.backend.service.PlayerScorecardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/player-scorecards")
@CrossOrigin(origins = "http://localhost:4200")
public class PlayerScorecardController {

    @Autowired
    private PlayerScorecardService playerScorecardService;

    @GetMapping
    public List<PlayerScorecard> getAllScorecards() {
        return playerScorecardService.getAllScorecards();
    }

    @GetMapping("/{id}")
    public Optional<PlayerScorecard> getScorecardById(@PathVariable Long id) {
        return playerScorecardService.getScorecardById(id);
    }

    @GetMapping("/by-tee-time/{roundTeeTimeId}")
    public List<PlayerScorecard> getScorecardsByRoundTeeTimeId(@PathVariable Long roundTeeTimeId) {
        return playerScorecardService.getScorecardsByRoundTeeTimeId(roundTeeTimeId);
    }

    @GetMapping("/by-tee-time/{roundTeeTimeId}/player/{playerId}")
    public List<PlayerScorecard> getScorecardsByRoundTeeTimeIdAndPlayerId(
            @PathVariable Long roundTeeTimeId,
            @PathVariable Long playerId) {
        return playerScorecardService.getScorecardsByRoundTeeTimeIdAndPlayerId(roundTeeTimeId, playerId);
    }

    @GetMapping("/by-tee-time/{roundTeeTimeId}/player/{playerId}/hole/{holeId}")
    public Optional<PlayerScorecard> getScorecardByRoundTeeTimeIdAndPlayerIdAndHoleId(
            @PathVariable Long roundTeeTimeId,
            @PathVariable Long playerId,
            @PathVariable Long holeId) {
        return playerScorecardService.getScorecardByRoundTeeTimeIdAndPlayerIdAndHoleId(roundTeeTimeId, playerId, holeId);
    }

    @GetMapping("/check/{roundTeeTimeId}")
    public boolean hasScorecards(@PathVariable Long roundTeeTimeId) {
        return playerScorecardService.hasScorecards(roundTeeTimeId);
    }

    @GetMapping("/total/{roundTeeTimeId}/{playerId}")
    public Integer calculateTotal(@PathVariable Long roundTeeTimeId, @PathVariable Long playerId) {
        return playerScorecardService.calculateTotal(roundTeeTimeId, playerId);
    }

    @PostMapping
    public PlayerScorecard createScorecard(@RequestBody PlayerScorecard scorecard) {
        return playerScorecardService.saveScorecard(scorecard);
    }

    @PutMapping("/{id}")
    public PlayerScorecard updateScorecard(@PathVariable Long id, @RequestBody PlayerScorecard scorecard) {
        scorecard.setId(id);
        return playerScorecardService.saveScorecard(scorecard);
    }

    @DeleteMapping("/{id}")
    public void deleteScorecard(@PathVariable Long id) {
        playerScorecardService.deleteScorecard(id);
    }
}
