package com.degen.backend.repository;

import com.degen.backend.entity.ScoringType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScoringTypeRepository extends JpaRepository<ScoringType, Long> {
}
