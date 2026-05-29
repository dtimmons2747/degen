package com.degen.backend.controller;

import com.degen.backend.entity.Hole;
import com.degen.backend.dto.HoleDTO;
import com.degen.backend.service.HoleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/holes")
@CrossOrigin(origins = "http://localhost:4200")
public class HoleController {

    private final HoleService holeService;

    public HoleController(HoleService holeService) {
        this.holeService = holeService;
    }

    @GetMapping
    public List<Hole> getAllHoles() {
        return holeService.getAllHoles();
    }

    @GetMapping("/{id}")
    public Optional<Hole> getHoleById(@PathVariable Long id) {
        return holeService.getHoleById(id);
    }

    @GetMapping(params = "courseId")
    public List<Hole> getHolesByCourseId(@RequestParam Long courseId) {
        return holeService.getHolesByCourseId(courseId);
    }

    @PostMapping
    public Hole createHole(@RequestBody HoleDTO holeDTO) {
        return holeService.createHoleFromDTO(holeDTO);
    }

    @PutMapping("/{id}")
    public Hole updateHole(@PathVariable Long id, @RequestBody HoleDTO holeDTO) {
        holeDTO.setId(id);
        return holeService.createHoleFromDTO(holeDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteHole(@PathVariable Long id) {
        holeService.deleteHole(id);
    }
}
