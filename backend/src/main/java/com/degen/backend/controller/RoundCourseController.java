package com.degen.backend.controller;

import com.degen.backend.entity.RoundCourse;
import com.degen.backend.service.RoundCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/round-courses")
@CrossOrigin(origins = "http://localhost:4200")
public class RoundCourseController {

    @Autowired
    private RoundCourseService roundCourseService;

    @GetMapping
    public List<RoundCourse> getAllRoundCourses() {
        return roundCourseService.getAllRoundCourses();
    }

    @GetMapping("/{id}")
    public Optional<RoundCourse> getRoundCourseById(@PathVariable Long id) {
        return roundCourseService.getRoundCourseById(id);
    }

    @PostMapping
    public RoundCourse createRoundCourse(@RequestBody RoundCourse roundCourse) {
        return roundCourseService.saveRoundCourse(roundCourse);
    }

    @PutMapping("/{id}")
    public RoundCourse updateRoundCourse(@PathVariable Long id, @RequestBody RoundCourse roundCourse) {
        roundCourse.setId(id);
        return roundCourseService.saveRoundCourse(roundCourse);
    }

    @DeleteMapping("/{id}")
    public void deleteRoundCourse(@PathVariable Long id) {
        roundCourseService.deleteRoundCourse(id);
    }
}
