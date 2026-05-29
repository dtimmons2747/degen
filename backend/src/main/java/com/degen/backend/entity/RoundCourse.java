package com.degen.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "round_course")
public class RoundCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "round_course_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tournament_round_id", referencedColumnName = "tournament_round_id")
    private TournamentRound tournamentRound;

    @ManyToOne
    @JoinColumn(name = "tournament_course_id", referencedColumnName = "tournament_course_id")
    private TournamentCourse tournamentCourse;

}
