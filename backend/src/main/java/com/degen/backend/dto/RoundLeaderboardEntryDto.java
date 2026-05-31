package com.degen.backend.dto;

public class RoundLeaderboardEntryDto {
    private Long playerId;
    private String playerName;
    private Integer score;           // net score (strokes - par)
    private Integer thru;            // holes completed (0-18)
    private Double roundPoints;      // points for this round
    private Double totalPoints;      // accumulated tournament points

    public RoundLeaderboardEntryDto(Long playerId, String playerName, Integer score, 
                                     Integer thru, Double roundPoints, Double totalPoints) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.score = score;
        this.thru = thru;
        this.roundPoints = roundPoints;
        this.totalPoints = totalPoints;
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

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getThru() {
        return thru;
    }

    public void setThru(Integer thru) {
        this.thru = thru;
    }

    public Double getRoundPoints() {
        return roundPoints;
    }

    public void setRoundPoints(Double roundPoints) {
        this.roundPoints = roundPoints;
    }

    public Double getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Double totalPoints) {
        this.totalPoints = totalPoints;
    }
}
