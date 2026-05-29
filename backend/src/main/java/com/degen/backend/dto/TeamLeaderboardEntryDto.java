package com.degen.backend.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamLeaderboardEntryDto {
    private Long teamId;
    private String teamName;
    private List<String> playerNames;
    private Map<Long, Double> roundPoints; // roundId -> points (can be fractional for ties)
    private Double totalPoints;

    public TeamLeaderboardEntryDto(Long teamId, String teamName, List<String> playerNames) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.playerNames = playerNames;
        this.roundPoints = new HashMap<>();
        this.totalPoints = 0.0;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public List<String> getPlayerNames() {
        return playerNames;
    }

    public void setPlayerNames(List<String> playerNames) {
        this.playerNames = playerNames;
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
