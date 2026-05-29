package com.degen.backend.dto;

import java.util.List;

public class HandicapCalculatorDto {
    private Long playerId;
    private String playerName;
    private Double handicap;
    private Integer roundsPlayed;
    private Integer holesPlayed;
    private Boolean eligible;
    private List<RoundDifferentialDto> roundDifferentials;

    public HandicapCalculatorDto() {
    }

    public HandicapCalculatorDto(Long playerId, String playerName, Double handicap, 
                                 Integer roundsPlayed, Integer holesPlayed, Boolean eligible) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.handicap = handicap;
        this.roundsPlayed = roundsPlayed;
        this.holesPlayed = holesPlayed;
        this.eligible = eligible;
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

    public Double getHandicap() {
        return handicap;
    }

    public void setHandicap(Double handicap) {
        this.handicap = handicap;
    }

    public Integer getRoundsPlayed() {
        return roundsPlayed;
    }

    public void setRoundsPlayed(Integer roundsPlayed) {
        this.roundsPlayed = roundsPlayed;
    }

    public Integer getHolesPlayed() {
        return holesPlayed;
    }

    public void setHolesPlayed(Integer holesPlayed) {
        this.holesPlayed = holesPlayed;
    }

    public Boolean getEligible() {
        return eligible;
    }

    public void setEligible(Boolean eligible) {
        this.eligible = eligible;
    }

    public List<RoundDifferentialDto> getRoundDifferentials() {
        return roundDifferentials;
    }

    public void setRoundDifferentials(List<RoundDifferentialDto> roundDifferentials) {
        this.roundDifferentials = roundDifferentials;
    }

    public static class RoundDifferentialDto {
        private Long roundTeeTimeId;
        private String roundDate;
        private String courseName;
        private Double courseRating;
        private Double slopeRating;
        private Integer grossScore;
        private Double scoreDifferential;
        private Integer holesPlayed;
        private Boolean isUsed;

        public RoundDifferentialDto(Long roundTeeTimeId, String roundDate, String courseName,
                                   Double courseRating, Double slopeRating, Integer grossScore,
                                   Double scoreDifferential, Integer holesPlayed, Boolean isUsed) {
            this.roundTeeTimeId = roundTeeTimeId;
            this.roundDate = roundDate;
            this.courseName = courseName;
            this.courseRating = courseRating;
            this.slopeRating = slopeRating;
            this.grossScore = grossScore;
            this.scoreDifferential = scoreDifferential;
            this.holesPlayed = holesPlayed;
            this.isUsed = isUsed;
        }

        public Long getRoundTeeTimeId() {
            return roundTeeTimeId;
        }

        public void setRoundTeeTimeId(Long roundTeeTimeId) {
            this.roundTeeTimeId = roundTeeTimeId;
        }

        public String getRoundDate() {
            return roundDate;
        }

        public void setRoundDate(String roundDate) {
            this.roundDate = roundDate;
        }

        public String getCourseName() {
            return courseName;
        }

        public void setCourseName(String courseName) {
            this.courseName = courseName;
        }

        public Double getCourseRating() {
            return courseRating;
        }

        public void setCourseRating(Double courseRating) {
            this.courseRating = courseRating;
        }

        public Double getSlopeRating() {
            return slopeRating;
        }

        public void setSlopeRating(Double slopeRating) {
            this.slopeRating = slopeRating;
        }

        public Integer getGrossScore() {
            return grossScore;
        }

        public void setGrossScore(Integer grossScore) {
            this.grossScore = grossScore;
        }

        public Double getScoreDifferential() {
            return scoreDifferential;
        }

        public void setScoreDifferential(Double scoreDifferential) {
            this.scoreDifferential = scoreDifferential;
        }

        public Integer getHolesPlayed() {
            return holesPlayed;
        }

        public void setHolesPlayed(Integer holesPlayed) {
            this.holesPlayed = holesPlayed;
        }

        public Boolean getIsUsed() {
            return isUsed;
        }

        public void setIsUsed(Boolean isUsed) {
            this.isUsed = isUsed;
        }
    }
}
