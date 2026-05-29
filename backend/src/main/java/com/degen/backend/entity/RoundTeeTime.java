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
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "round_tee_time")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RoundTeeTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "round_tee_time_id")
    private Long id;

    @Column(name = "tee_time")
    private LocalDateTime teeTime;

    @Transient
    @JsonProperty("tournamentRoundId")
    private Long tournamentRoundId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_round_id", referencedColumnName = "tournament_round_id", nullable = false)
    private TournamentRound tournamentRound;

    @Column(name = "player1_id")
    private Long player1Id;

    @Column(name = "player2_id")
    private Long player2Id;

    @Column(name = "player3_id")
    private Long player3Id;

    @Column(name = "player4_id")
    private Long player4Id;

    // Transient fields for receiving handicap data from frontend
    // These are converted to RoundHandicap records in the service layer
    @Transient
    private Double player1Handicap;

    @Transient
    private Double player2Handicap;

    @Transient
    private Double player3Handicap;

    @Transient
    private Double player4Handicap;
}
