package com.degen.backend.service;

import com.degen.backend.entity.Hole;
import com.degen.backend.repository.HoleRepository;
import com.degen.backend.repository.CourseRepository;
import com.degen.backend.dto.HoleDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HoleService {

    private final HoleRepository holeRepository;
    private final CourseRepository courseRepository;

    public HoleService(HoleRepository holeRepository, CourseRepository courseRepository) {
        this.holeRepository = holeRepository;
        this.courseRepository = courseRepository;
    }

    public List<Hole> getAllHoles() {
        return holeRepository.findAll();
    }

    public Optional<Hole> getHoleById(Long id) {
        return holeRepository.findById(id);
    }

    public List<Hole> getHolesByCourseId(Long courseId) {
        return holeRepository.findByCourseId(courseId);
    }

    public Hole saveHole(Hole hole) {
        // Ensure course is set if hole has a course object
        if (hole.getCourse() == null && hole.getId() == null) {
            throw new IllegalArgumentException("Hole must have a course reference");
        }
        return holeRepository.save(hole);
    }

    public Hole createHoleFromDTO(HoleDTO holeDTO) {
        Hole hole = new Hole();
        hole.setId(holeDTO.getId());
        hole.setHoleNumber(holeDTO.getHoleNumber());
        hole.setPar(holeDTO.getPar());
        hole.setYards(holeDTO.getYards());
        hole.setHandicap(holeDTO.getHandicap());

        // Fetch and set the course
        if (holeDTO.getCourseId() != null) {
            var course = courseRepository.findById(holeDTO.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + holeDTO.getCourseId()));
            hole.setCourse(course);
        } else {
            throw new IllegalArgumentException("Course ID must be provided when creating a hole");
        }

        return holeRepository.save(hole);
    }

    public void deleteHole(Long id) {
        holeRepository.deleteById(id);
    }
}
