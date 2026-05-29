package com.degen.backend.repository;

import com.degen.backend.entity.Hole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoleRepository extends JpaRepository<Hole, Long> {
    
    @Query("SELECT h FROM Hole h WHERE h.course.id = :courseId ORDER BY h.holeNumber")
    List<Hole> findByCourseId(@Param("courseId") Long courseId);
}
