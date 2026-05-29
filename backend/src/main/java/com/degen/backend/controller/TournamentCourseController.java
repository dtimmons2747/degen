package com.degen.backend.controller;

import com.degen.backend.entity.TournamentCourse;
import com.degen.backend.service.TournamentCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tournament-courses")
@CrossOrigin(origins = "http://localhost:4200")
public class TournamentCourseController {

    @Autowired
    private TournamentCourseService tournamentCourseService;

    @GetMapping
    public List<TournamentCourse> getTournamentCourses(@RequestParam(required = false) Long tournamentId) {
        if (tournamentId != null) {
            return tournamentCourseService.getTournamentCoursesByTournamentId(tournamentId);
        }
        return tournamentCourseService.getAllTournamentCourses();
    }

    @GetMapping("/{id}")
    public Optional<TournamentCourse> getTournamentCourseById(@PathVariable Long id) {
        return tournamentCourseService.getTournamentCourseById(id);
    }

    @PostMapping
    public TournamentCourse createTournamentCourse(@RequestBody TournamentCourse tournamentCourse) {
        return tournamentCourseService.saveTournamentCourse(tournamentCourse);
    }

    @PutMapping("/{id}")
    public TournamentCourse updateTournamentCourse(@PathVariable Long id, @RequestBody TournamentCourse tournamentCourse) {
        tournamentCourse.setId(id);
        return tournamentCourseService.saveTournamentCourse(tournamentCourse);
    }

    @DeleteMapping("/{id}")
    public void deleteTournamentCourse(@PathVariable Long id) {
        tournamentCourseService.deleteTournamentCourse(id);
    }
}
