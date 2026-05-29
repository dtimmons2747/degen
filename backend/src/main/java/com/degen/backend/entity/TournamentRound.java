package com.degen.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.ForeignKey;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tournament_round")
public class TournamentRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tournament_round_id")
    private Long id;

    @Column(name = "day")
    private LocalDate day;

    @ManyToOne
    @JoinColumn(name = "game_id", referencedColumnName = "game_id")
    private Game game;

    @ManyToOne
    @JoinColumn(name = "tournament_id", referencedColumnName = "id")
    private Tournament tournament;

    @ManyToOne
    @JoinColumn(name = "course_id", referencedColumnName = "course_id", nullable = true,
        foreignKey = @ForeignKey(name = "FK_tournament_round_course"))
    private Course course;

    @ManyToOne
    @JoinColumn(name = "scoring_type_id", referencedColumnName = "scoring_type_id", nullable = true)
    private ScoringType scoringType;

    @JsonProperty("split_skins")
    @Column(name = "split_skins")
    private Boolean splitSkins;

    @JsonProperty("vs_group")
    @Column(name = "vs_group")
    private Boolean vsGroup;

    @JsonIgnore
    @OneToMany(mappedBy = "tournamentRound", fetch = FetchType.LAZY)
    private List<RoundTeeTime> teeTimes;
}
