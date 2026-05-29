package com.degen.backend.dto;

import java.util.HashMap;
import java.util.Map;

public class LeaderboardEntryDto {
    private Long playerId;
    private String playerName;
    private Map<Long, Double> roundPoints; // roundId -> points (can be fractional for ties)
    private Double totalPoints;

    public LeaderboardEntryDto(Long playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.roundPoints = new HashMap<>();
        this.totalPoints = 0.0;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Map<Long, Double> getRoundPoints() {
        return roundPoints;
    }

    public void setRoundPoints(Map<Long, Double> roundPoints) {
        this.roundPoints = roundPoints;
    }

    public Double getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Double totalPoints) {
        this.totalPoints = totalPoints;
    }

    public void addRoundPoints(Long roundId, Double points) {
        this.roundPoints.merge(roundId, points, Double::sum);
        this.totalPoints += points;
    }
}
