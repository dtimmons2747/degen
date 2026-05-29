package com.degen.backend.service;

import com.degen.backend.entity.RoundCourse;
import com.degen.backend.repository.RoundCourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoundCourseService {

    @Autowired
    private RoundCourseRepository roundCourseRepository;

    public List<RoundCourse> getAllRoundCourses() {
        return roundCourseRepository.findAll();
    }

    public Optional<RoundCourse> getRoundCourseById(Long id) {
        return roundCourseRepository.findById(id);
    }

    public RoundCourse saveRoundCourse(RoundCourse roundCourse) {
        return roundCourseRepository.save(roundCourse);
    }

    public void deleteRoundCourse(Long id) {
        roundCourseRepository.deleteById(id);
    }
}
