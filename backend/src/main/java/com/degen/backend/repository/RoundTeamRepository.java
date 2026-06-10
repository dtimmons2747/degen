package com.degen.backend.repository;

import com.degen.backend.entity.RoundTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundTeamRepository extends JpaRepository<RoundTeam, Long> {

    @Query("SELECT rt FROM RoundTeam rt WHERE rt.roundTeeTime.id = :roundTeeTimeId")
    List<RoundTeam> findByRoundTeeTimeId(@Param("roundTeeTimeId") Long roundTeeTimeId);

    @Modifying
    @Query("DELETE FROM RoundTeam rt WHERE rt.roundTeeTime.id = :roundTeeTimeId")
    void deleteByRoundTeeTimeId(@Param("roundTeeTimeId") Long roundTeeTimeId);
}
