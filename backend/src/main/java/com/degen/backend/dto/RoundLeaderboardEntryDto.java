package com.degen.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class RoundLeaderboardEntryDto {
  private Long playerId;
  private String playerName;
  private Integer score; // net score (strokes - par)
  private Integer thru; // holes completed (0-18)
  private Double roundPoints; // points for this round
  private Double totalPoints; // accumulated tournament points

  public RoundLeaderboardEntryDto(Long playerId, String playerName, Integer score,
      Integer thru, Double roundPoints, Double totalPoints) {
    this.playerId = playerId;
    this.playerName = playerName;
    this.score = score;
    this.thru = thru;
    this.roundPoints = roundPoints;
    this.totalPoints = totalPoints;
  }
}
