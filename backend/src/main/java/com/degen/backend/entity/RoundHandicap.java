package com.degen.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "round_handicap", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "round_tee_time_id", "player_id" })
})
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RoundHandicap {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "round_handicap_id")
  private Long id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "round_tee_time_id", nullable = false)
  private RoundTeeTime roundTeeTime;

  @Column(name = "player_id", nullable = false)
  private Long playerId;

  @Column(name = "handicap")
  private Double handicap;
}
