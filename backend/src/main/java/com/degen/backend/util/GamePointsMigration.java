package com.degen.backend.util;

import com.degen.backend.entity.PlayerScorecard;
import com.degen.backend.repository.PlayerScorecardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Migration script to calculate and save game_points for all existing player scorecards.
 * 
 * This script:
 * 1. Retrieves all player scorecards with gamePoints = null
 * 2. Groups scorecards by tee_time and hole
 * 3. Calculates gamePoints based on Nines game rules (3 players, 9 points per hole)
 * 4. Updates the database with calculated gamePoints
 * 
 * Nines Scoring Rules:
 * - Standard (no ties): 1st=5, 2nd=3, 3rd=1
 * - Tie for 1st: each=4, 3rd=1
 * - All tied: each=3
 * - Tie for 2nd: 1st=5, tied 2nd=2 each
 * 
 * To run this migration:
 * - Uncomment @Component annotation below
 * - Start the Spring Boot application
 * - The migration will run automatically on startup
 * - Comment out @Component again after successful migration
 */
//@Component
public class GamePointsMigration implements CommandLineRunner {

    @Autowired
    private PlayerScorecardRepository playerScorecardRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("========== Starting Game Points Migration ==========");
        
        // Get all scorecards
        List<PlayerScorecard> allScorecards = playerScorecardRepository.findAll();
        
        // Group by tee_time and hole
        Map<Long, Map<Long, List<PlayerScorecard>>> scoresByTeeTimeAndHole = new HashMap<>();
        
        for (PlayerScorecard scorecard : allScorecards) {
            // Skip if already has gamePoints
            if (scorecard.getGamePoints() != null) {
                continue;
            }
            
            // Skip if missing required fields
            if (scorecard.getNetScore() == null || 
                scorecard.getHole() == null ||
                scorecard.getRoundTeeTime() == null) {
                continue;
            }
            
            Long teeTimeId = scorecard.getRoundTeeTime().getId();
            Long holeId = scorecard.getHole().getId();
            
            scoresByTeeTimeAndHole
                .computeIfAbsent(teeTimeId, k -> new HashMap<>())
                .computeIfAbsent(holeId, k -> new ArrayList<>())
                .add(scorecard);
        }
        
        int updated = 0;
        int skipped = 0;
        
        // Process each hole per tee_time
        for (Long teeTimeId : scoresByTeeTimeAndHole.keySet()) {
            for (Long holeId : scoresByTeeTimeAndHole.get(teeTimeId).keySet()) {
                List<PlayerScorecard> holeScores = scoresByTeeTimeAndHole.get(teeTimeId).get(holeId);
                
                // Nines game requires exactly 3 players
                if (holeScores.size() != 3) {
                    System.out.println("SKIP: Hole " + holeId + " in tee_time " + teeTimeId + 
                        " - Expected 3 players, found " + holeScores.size());
                    skipped += holeScores.size();
                    continue;
                }
                
                // Sort by net score (ascending = best score first)
                List<PlayerScorecard> sorted = holeScores.stream()
                    .sorted(Comparator.comparingInt(sc -> sc.getNetScore() != null ? sc.getNetScore() : Integer.MAX_VALUE))
                    .collect(Collectors.toList());
                
                // Calculate game points for each player
                List<Integer> gamePoints = calculateGamePointsForHole(sorted);
                
                for (int i = 0; i < sorted.size(); i++) {
                    PlayerScorecard scorecard = sorted.get(i);
                    scorecard.setGamePoints(gamePoints.get(i));
                    playerScorecardRepository.save(scorecard);
                    updated++;
                    
                    System.out.println("UPDATE: Scorecard ID " + scorecard.getId() + 
                        " - Hole: " + holeId + 
                        ", Player: " + scorecard.getPlayer().getId() + 
                        ", Net Score: " + scorecard.getNetScore() + 
                        ", Game Points: " + gamePoints.get(i));
                }
            }
        }
        
        System.out.println("========== Game Points Migration Complete ==========");
        System.out.println("Updated: " + updated);
        System.out.println("Skipped: " + skipped);
        System.out.println("Total: " + (updated + skipped));
    }

    /**
     * Calculate game points for 3 scorecards on a hole using Nines rules.
     * Input list must be sorted by net score (ascending).
     * 
     * Nines Scoring Rules (3 players, 9 points total):
     * - Standard (no ties): 1st=5, 2nd=3, 3rd=1
     * - Tie for 1st: each=4, 3rd=1
     * - All tied: each=3
     * - Tie for 2nd: 1st=5, tied 2nd=2 each
     */
    private List<Integer> calculateGamePointsForHole(List<PlayerScorecard> sortedByNetScore) {
        List<Integer> points = new ArrayList<>(Arrays.asList(0, 0, 0));
        
        if (sortedByNetScore.size() != 3) {
            return points;
        }
        
        Integer score1 = sortedByNetScore.get(0).getNetScore();
        Integer score2 = sortedByNetScore.get(1).getNetScore();
        Integer score3 = sortedByNetScore.get(2).getNetScore();
        
        // All tied
        if (score1.equals(score2) && score2.equals(score3)) {
            return Arrays.asList(3, 3, 3);
        }
        
        // Tie for first place (1st and 2nd tied, 3rd different)
        if (score1.equals(score2) && !score2.equals(score3)) {
            return Arrays.asList(4, 4, 1);
        }
        
        // Tie for second place (1st different, 2nd and 3rd tied)
        if (!score1.equals(score2) && score2.equals(score3)) {
            return Arrays.asList(5, 2, 2);
        }
        
        // Standard: no ties
        return Arrays.asList(5, 3, 1);
    }
}
