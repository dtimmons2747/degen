package com.degen.backend.repository;

import com.degen.backend.entity.TournamentCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentCourseRepository extends JpaRepository<TournamentCourse, Long> {
    
    @Query("SELECT tc FROM TournamentCourse tc WHERE tc.tournament.id = :tournamentId ORDER BY tc.tournament.year DESC")
    List<TournamentCourse> findByTournamentId(@Param("tournamentId") Long tournamentId);
}
