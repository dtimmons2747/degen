package com.degen.backend.service;

import com.degen.backend.entity.ScoringType;
import com.degen.backend.repository.ScoringTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ScoringTypeService {

    @Autowired
    private ScoringTypeRepository scoringTypeRepository;

    public List<ScoringType> getAllScoringTypes() {
        return scoringTypeRepository.findAll();
    }

    public Optional<ScoringType> getScoringTypeById(Long id) {
        return scoringTypeRepository.findById(id);
    }

    public ScoringType saveScoringType(ScoringType scoringType) {
        return scoringTypeRepository.save(scoringType);
    }

    public void deleteScoringType(Long id) {
        scoringTypeRepository.deleteById(id);
    }
}
