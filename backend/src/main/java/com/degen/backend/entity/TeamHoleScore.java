package com.degen.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "team_hole_score")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TeamHoleScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_hole_score_id")
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "round_team_id", referencedColumnName = "round_team_id", nullable = false)
    private RoundTeam roundTeam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hole_id", referencedColumnName = "hole_id", nullable = false)
    private Hole hole;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "tournament_round_id", referencedColumnName = "tournament_round_id", nullable = false)
    private TournamentRound tournamentRound;

    @Column(name = "net_score", nullable = false)
    private Integer netScore; // Best net score in team for this hole

    @Column(name = "game_points", nullable = true)
    private Integer gamePoints; // Points earned for this hole

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
