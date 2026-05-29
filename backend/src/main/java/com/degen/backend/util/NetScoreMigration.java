package com.degen.backend.util;

import com.degen.backend.entity.Hole;
import com.degen.backend.entity.PlayerScorecard;
import com.degen.backend.entity.TournamentHandicap;
import com.degen.backend.repository.HoleRepository;
import com.degen.backend.repository.PlayerScorecardRepository;
import com.degen.backend.repository.TournamentHandicapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Migration script to calculate and save net_score for all existing player scorecards.
 * 
 * This script:
 * 1. Retrieves all player scorecards with net_score = null
 * 2. Calculates net_score based on gross_score, hole handicap, and player's tournament handicap
 * 3. Updates the database with calculated net_score
 * 
 * To run this migration:
 * - Uncomment @Component annotation below
 * - Start the Spring Boot application
 * - The migration will run automatically on startup
 * - Comment out @Component again after successful migration
 */
 //@Component
public class NetScoreMigration implements CommandLineRunner {

    @Autowired
    private PlayerScorecardRepository playerScorecardRepository;

    @Autowired
    private TournamentHandicapRepository tournamentHandicapRepository;

    @Autowired
    private HoleRepository holeRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("========== Starting Net Score Migration ==========");
        
        // Get all scorecards that don't have a net_score yet
        List<PlayerScorecard> scorecards = playerScorecardRepository.findAll();
        
        int updated = 0;
        int skipped = 0;
        
        for (PlayerScorecard scorecard : scorecards) {
            try {
                // Skip if already has net_score
                if (scorecard.getNetScore() != null) {
                    skipped++;
                    continue;
                }
                
                // Skip if missing required fields
                if (scorecard.getGrossScore() == null || 
                    scorecard.getHole() == null || 
                    scorecard.getPlayer() == null ||
                    scorecard.getRoundTeeTime() == null) {
                    System.out.println("SKIP: Scorecard ID " + scorecard.getId() + " - Missing required fields");
                    skipped++;
                    continue;
                }
                
                // Get the tournament from roundTeeTime
                Long tournamentId = scorecard.getRoundTeeTime().getTournamentRound().getTournament().getId();
                Long playerId = scorecard.getPlayer().getId();
                
                // Get tournament handicap for the player
                List<TournamentHandicap> handicaps = tournamentHandicapRepository
                    .findByTournamentIdAndPlayerId(tournamentId, playerId);
                
                if (handicaps.isEmpty()) {
                    System.out.println("SKIP: Scorecard ID " + scorecard.getId() + 
                        " - No tournament handicap found for player " + playerId + 
                        " in tournament " + tournamentId);
                    skipped++;
                    continue;
                }
                
                // Use first handicap found (should be only one per tournament)
                Double playerHandicap = handicaps.get(0).getHandicap();
                if (playerHandicap == null) {
                    playerHandicap = 0.0;
                }
                
                // Calculate course handicap using formula: handicap * (slope / 113) + (rating - par)
                Double courseHandicap = calculateCourseHandicap(
                    playerHandicap,
                    scorecard.getRoundTeeTime().getTournamentRound()
                );
                
                // Calculate net score
                Integer netScore = calculateNetScore(
                    scorecard.getGrossScore(),
                    courseHandicap.intValue(),
                    scorecard.getHole().getHandicap()
                );
                
                // Update scorecard
                scorecard.setNetScore(netScore);
                playerScorecardRepository.save(scorecard);
                updated++;
                
                System.out.println("UPDATE: Scorecard ID " + scorecard.getId() + 
                    " - Gross: " + scorecard.getGrossScore() + 
                    ", Net: " + netScore + 
                    ", Player Handicap: " + courseHandicap +
                    ", Hole Handicap: " + scorecard.getHole().getHandicap());
                
            } catch (Exception e) {
                System.err.println("ERROR processing scorecard ID " + scorecard.getId() + ": " + e.getMessage());
                e.printStackTrace();
                skipped++;
            }
        }
        
        System.out.println("========== Net Score Migration Complete ==========");
        System.out.println("Updated: " + updated);
        System.out.println("Skipped: " + skipped);
        System.out.println("Total: " + (updated + skipped));
    }

    /**
     * Calculate course handicap using the formula: 
     * courseHandicap = (playerHandicap * slope / 113) + (rating - coursePar)
     */
    private Double calculateCourseHandicap(Double playerHandicap, Object tournamentRound) {
        try {
            // Get course info from tournament round
            java.lang.reflect.Method getMethod = tournamentRound.getClass().getMethod("getCourse");
            Object course = getMethod.invoke(tournamentRound);
            
            if (course == null) {
                return playerHandicap; // Fallback to player handicap if no course info
            }
            
            // Get slope and rating from course
            java.lang.reflect.Method getSlopeMethod = course.getClass().getMethod("getSlope");
            java.lang.reflect.Method getRatingMethod = course.getClass().getMethod("getRating");
            java.lang.reflect.Method getCourseIdMethod = course.getClass().getMethod("getId");
            
            Object slope = getSlopeMethod.invoke(course);
            Object rating = getRatingMethod.invoke(course);
            Object courseId = getCourseIdMethod.invoke(course);
            
            if (slope == null || rating == null || courseId == null) {
                return playerHandicap; // Fallback if missing course data
            }
            
            // Calculate course par from all holes
            List<Hole> courseHoles = holeRepository.findByCourseId((Long) courseId);
            int coursePar = 0;
            for (Hole hole : courseHoles) {
                if (hole.getPar() != null) {
                    coursePar += hole.getPar();
                }
            }
            
            if (coursePar == 0) {
                return playerHandicap; // Fallback if no holes found
            }
            
            // Apply formula: handicap * (slope / 113) + (rating - par)
            double slopeValue = ((Number) slope).doubleValue();
            double ratingValue = ((Number) rating).doubleValue();
            double courseHandicap = (playerHandicap * (slopeValue / 113.0)) + (ratingValue - coursePar);
            
            return courseHandicap;
        } catch (Exception e) {
            System.err.println("Error calculating course handicap: " + e.getMessage());
            return playerHandicap; // Fallback to player handicap on error
        }
    }

    /**
     * Calculate net score based on gross score, course handicap, and hole handicap.
     * 
     * Logic:
     * - If handicap is 18, subtract 1 stroke from each hole
     * - If handicap is 36, subtract 2 strokes from each hole
     * - If handicap is 11, subtract 1 stroke from the 11 hardest holes (highest handicap)
     */
    private Integer calculateNetScore(Integer grossScore, Integer courseHandicap, Integer holeHandicap) {
        if (grossScore == null || courseHandicap == null || holeHandicap == null) {
            return null;
        }

        int baseSubtraction = courseHandicap / 18;
        int remainingSubtraction = courseHandicap % 18;
        
        // Additional stroke if this hole is among the remaining hardest holes
        int additionalStroke = (holeHandicap <= remainingSubtraction) ? 1 : 0;
        
        int totalStrokesToSubtract = baseSubtraction + additionalStroke;
        int netScore = grossScore - totalStrokesToSubtract;
        
        return Math.max(0, netScore);
    }
}
