package com.degen.backend.service;

import com.degen.backend.dto.HandicapCalculatorDto;
import com.degen.backend.entity.*;
import com.degen.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HandicapCalculatorService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerScorecardRepository playerScorecardRepository;

    private static final int MINIMUM_HOLES_FOR_INITIAL = 54;
    private static final int ROUNDS_TO_CONSIDER = 20;
    private static final double WHS_CONSTANT = 113.0;
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public HandicapCalculatorDto calculateHandicap(Long playerId) {
        Optional<Player> playerOpt = playerRepository.findById(playerId);
        if (!playerOpt.isPresent()) {
            return null;
        }

        Player player = playerOpt.get();
        List<PlayerScorecard> scorecards = playerScorecardRepository
                .findPlayerScorecardsByPlayerIdOrderByDate(playerId);

        if (scorecards.isEmpty()) {
            HandicapCalculatorDto dto = new HandicapCalculatorDto(
                    playerId,
                    player.getFirstName() + " " + player.getLastName(),
                    null,
                    0,
                    0,
                    false);
            dto.setRoundDifferentials(new ArrayList<>());
            return dto;
        }

        // Group scorecards by round (by round_tee_time_id and tournament_round_id)
        Map<String, List<PlayerScorecard>> roundsMap = groupScorecardsByRound(scorecards);
        List<RoundData> rounds = new ArrayList<>();

        for (Map.Entry<String, List<PlayerScorecard>> entry : roundsMap.entrySet()) {
            List<PlayerScorecard> roundScorecards = entry.getValue();
            if (!roundScorecards.isEmpty()) {
                RoundData roundData = calculateRoundDifferential(roundScorecards);
                if (roundData != null) {
                    rounds.add(roundData);
                }
            }
        }

        // Sort by most recent first, take last 20
        rounds.sort((a, b) -> b.roundDate.compareTo(a.roundDate));
        if (rounds.size() > ROUNDS_TO_CONSIDER) {
            rounds = rounds.subList(0, ROUNDS_TO_CONSIDER);
        }

        // Calculate handicap
        int totalHoles = rounds.stream().mapToInt(r -> r.holesPlayed).sum();
        boolean eligible = totalHoles >= MINIMUM_HOLES_FOR_INITIAL;

        Double handicap = null;
        Set<Double> usedDifferentials = new HashSet<>();
        if (eligible && !rounds.isEmpty()) {
            int numRounds = rounds.size();
            int scoresToUse = getScoresToUse(numRounds);
            double adjustment = getPlayingConditionsAdjustment(numRounds);

            // Get the best differentials based on WHS rules
            List<Double> differentials = rounds.stream()
                    .map(r -> r.scoreDifferential)
                    .filter(Objects::nonNull)
                    .sorted()
                    .limit(scoresToUse)
                    .collect(Collectors.toList());

            usedDifferentials.addAll(differentials);

            if (!differentials.isEmpty()) {
                double average = differentials.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                handicap = Math.round((average + adjustment) * 10.0) / 10.0; // Round to nearest tenth
            }
        }

        List<HandicapCalculatorDto.RoundDifferentialDto> roundDifferentials = rounds.stream()
                .map(r -> new HandicapCalculatorDto.RoundDifferentialDto(
                        r.roundTeeTimeId,
                        r.roundDate.format(dateFormatter),
                        r.courseName,
                        r.courseRating,
                        r.slopeRating,
                        r.grossScore,
                        r.scoreDifferential,
                        r.holesPlayed,
                        usedDifferentials.contains(r.scoreDifferential)))
                .collect(Collectors.toList());

        HandicapCalculatorDto dto = new HandicapCalculatorDto(
                playerId,
                player.getFirstName() + " " + player.getLastName(),
                handicap,
                rounds.size(),
                totalHoles,
                eligible);
        dto.setRoundDifferentials(roundDifferentials);

        return dto;
    }

    private Map<String, List<PlayerScorecard>> groupScorecardsByRound(List<PlayerScorecard> scorecards) {
        Map<String, List<PlayerScorecard>> roundsMap = new LinkedHashMap<>();

        for (PlayerScorecard scorecard : scorecards) {
            String key = scorecard.getRoundTeeTime().getId() + "_" +
                    scorecard.getRoundTeeTime().getTournamentRound().getId();
            roundsMap.computeIfAbsent(key, k -> new ArrayList<>()).add(scorecard);
        }

        return roundsMap;
    }

    private RoundData calculateRoundDifferential(List<PlayerScorecard> roundScorecards) {
        if (roundScorecards.isEmpty()) {
            return null;
        }

        PlayerScorecard firstCard = roundScorecards.get(0);
        RoundTeeTime roundTeeTime = firstCard.getRoundTeeTime();
        TournamentRound tournamentRound = roundTeeTime.getTournamentRound();
        Course course = tournamentRound.getCourse();

        if (course == null || course.getRating() == null || course.getSlope() == null) {
            return null;
        }

        // Calculate gross score (sum of all holes' gross scores)
        Integer grossScore = roundScorecards.stream()
                .mapToInt(ps -> ps.getGrossScore() != null ? ps.getGrossScore() : 0)
                .sum();

        // Get par total for this round
        int parTotal = roundScorecards.stream()
                .mapToInt(ps -> ps.getHole().getPar() != null ? ps.getHole().getPar() : 0)
                .sum();

        // Calculate handicap strokes total for this round
        double handicapStrokesTotal = 0;
        for (PlayerScorecard ps : roundScorecards) {
            Integer holeHandicap = ps.getHole().getHandicap();
            if (holeHandicap != null && course.getSlope() != null) {
                // Handicap strokes = (Hole Handicap × Slope Rating / 113) rounded down
                double handicapStrokes = Math.floor((holeHandicap * course.getSlope()) / 113.0);
                handicapStrokesTotal += handicapStrokes;
            }
        }

        // Calculate adjusted gross score (Net Double Bogey adjustment)
        // Adjusted Gross = Gross Score (capped at Par + 2 + handicap strokes per hole)
        int adjustedGrossScore = grossScore;
        for (PlayerScorecard ps : roundScorecards) {
            Integer score = ps.getGrossScore();
            Hole hole = ps.getHole();
            if (score != null && hole.getPar() != null && hole.getHandicap() != null) {
                double holeHandicap = Math.floor((hole.getHandicap() * course.getSlope()) / 113.0);
                int maxScore = hole.getPar() + 2 + (int) holeHandicap;
                if (score > maxScore) {
                    adjustedGrossScore = adjustedGrossScore - score + maxScore;
                }
            }
        }

        // Calculate score differential using WHS formula
        // Score Differential = (Adjusted Gross Score - Course Rating) / Slope Rating ×
        // 113
        double scoreDifferential = ((adjustedGrossScore - course.getRating()) / course.getSlope()) * WHS_CONSTANT;

        RoundData roundData = new RoundData();
        roundData.roundTeeTimeId = roundTeeTime.getId();
        roundData.roundDate = tournamentRound.getDay();
        roundData.courseName = course.getName();
        roundData.courseRating = course.getRating();
        roundData.slopeRating = course.getSlope();
        roundData.grossScore = grossScore;
        roundData.scoreDifferential = Math.round(scoreDifferential * 10.0) / 10.0; // Round to nearest tenth
        roundData.holesPlayed = roundScorecards.size();

        return roundData;
    }

    /**
     * Determine how many of the best score differentials to use for handicap
     * calculation
     * based on WHS Playing Conditions Calculation table
     */
    private int getScoresToUse(int numRounds) {
        if (numRounds <= 5)
            return 1;
        if (numRounds <= 8)
            return 2;
        if (numRounds <= 11)
            return 2;
        if (numRounds <= 14)
            return 3;
        if (numRounds <= 16)
            return 4;
        if (numRounds <= 18)
            return 5;
        if (numRounds == 19)
            return 6;
        return 7; // 20+ rounds
    }

    /**
     * Get the Playing Conditions Calculation (PCC) adjustment factor
     * based on the number of rounds played (WHS table)
     */
    private double getPlayingConditionsAdjustment(int numRounds) {
        if (numRounds == 3)
            return -2.0;
        if (numRounds == 4)
            return -1.0;
        if (numRounds == 5)
            return 0.0;
        if (numRounds == 6)
            return -1.0;
        return 0.0; // 7+ rounds
    }

    private static class RoundData {
        Long roundTeeTimeId;
        java.time.LocalDate roundDate;
        String courseName;
        Double courseRating;
        Double slopeRating;
        Integer grossScore;
        Double scoreDifferential;
        Integer holesPlayed;
    }
}
