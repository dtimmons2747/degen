package com.degen.backend.repository;

import com.degen.backend.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    
    @Query("SELECT t FROM Tournament t ORDER BY t.year DESC")
    List<Tournament> findAll();
}