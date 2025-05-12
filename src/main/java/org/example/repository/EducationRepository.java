package org.example.repository;

import org.example.model.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EducationRepository extends JpaRepository<Education, Long> {
    List<Education> findByEmployeeId(Long employeeId);

    @Query("DELETE FROM Education e WHERE e.employeeId = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") Long employeeId);
}