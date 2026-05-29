package com.degen.backend.controller;

import com.degen.backend.entity.ScoringType;
import com.degen.backend.service.ScoringTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/scoring-types")
@CrossOrigin(origins = "http://localhost:4200")
public class ScoringTypeController {

    @Autowired
    private ScoringTypeService scoringTypeService;

    @GetMapping
    public List<ScoringType> getScoringTypes() {
        return scoringTypeService.getAllScoringTypes();
    }

    @GetMapping("/{id}")
    public Optional<ScoringType> getScoringTypeById(@PathVariable Long id) {
        return scoringTypeService.getScoringTypeById(id);
    }

    @PostMapping
    public ScoringType createScoringType(@RequestBody ScoringType scoringType) {
        return scoringTypeService.saveScoringType(scoringType);
    }

    @PutMapping("/{id}")
    public ScoringType updateScoringType(@PathVariable Long id, @RequestBody ScoringType scoringType) {
        scoringType.setId(id);
        return scoringTypeService.saveScoringType(scoringType);
    }

    @DeleteMapping("/{id}")
    public void deleteScoringType(@PathVariable Long id) {
        scoringTypeService.deleteScoringType(id);
    }
}
