package com.degen.backend.service;

import com.degen.backend.entity.TournamentCourse;
import com.degen.backend.repository.TournamentCourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TournamentCourseService {

    @Autowired
    private TournamentCourseRepository tournamentCourseRepository;

    public List<TournamentCourse> getAllTournamentCourses() {
        return tournamentCourseRepository.findAll();
    }

    public Optional<TournamentCourse> getTournamentCourseById(Long id) {
        return tournamentCourseRepository.findById(id);
    }

    public List<TournamentCourse> getTournamentCoursesByTournamentId(Long tournamentId) {
        return tournamentCourseRepository.findByTournamentId(tournamentId);
    }

    public TournamentCourse saveTournamentCourse(TournamentCourse tournamentCourse) {
        return tournamentCourseRepository.save(tournamentCourse);
    }

    public void deleteTournamentCourse(Long id) {
        tournamentCourseRepository.deleteById(id);
    }
}
