/*package org.example.repository;

import org.example.model.Employee;
import org.example.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EmployeeRepositoryJPA extends JpaRepository<Employee, Long> {

    // === СУЩЕСТВУЮЩИЕ МЕТОДЫ ===

    @Query("SELECT e FROM Employee e WHERE " +
            "(:name = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:position = '' OR e.position = :position) AND " +
            "(:department = '' OR e.department = :department)")
    Page<Employee> findByNameContainingAndPositionAndDepartment(
            @Param("name") String name,
            @Param("position") String position,
            @Param("department") String department,
            Pageable pageable
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO employee_skills (employee_id, skill) VALUES (:employeeId, :skill)",
            nativeQuery = true)
    void addSkillToEmployee(@Param("employeeId") Long employeeId,
                            @Param("skill") String skill);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM employee_skills WHERE employee_id = :employeeId AND skill = :skill",
            nativeQuery = true)
    void removeSkillFromEmployee(@Param("employeeId") Long employeeId,
                                 @Param("skill") String skill);

    @Query(value = "SELECT skill FROM employee_skills WHERE employee_id = :employeeId",
            nativeQuery = true)
    Set<String> findSkillsByEmployeeId(@Param("employeeId") Long employeeId);

    @Query(value = "SELECT COUNT(*) > 0 FROM employee_skills WHERE employee_id = :employeeId AND skill = :skill",
            nativeQuery = true)
    boolean hasSkill(@Param("employeeId") Long employeeId,
                     @Param("skill") String skill);

    Optional<Employee> findByName(String name);
    Optional<Employee> findByEmail(String name);
    Page<Employee> findByNameContaining(String name, Pageable pageable);

    long countByNameContaining(String name);
    // Дополнительный метод для фильтрации по категориям (если нужно)

    @Query("SELECT e FROM Employee e JOIN e.skills s WHERE s IN :skillNames")
    List<Employee> findBySkillsIn(@Param("skillNames") List<String> skillNames);

    // === НОВЫЕ МЕТОДЫ ДЛЯ ФИЛЬТРАЦИИ ===

    @Query("SELECT DISTINCT e.department FROM Employee e WHERE e.department IS NOT NULL ORDER BY e.department")
    List<String> findDistinctDepartments();

    @Query("SELECT DISTINCT e.position FROM Employee e WHERE e.position IS NOT NULL ORDER BY e.position")
    List<String> findDistinctPositions();

    // Метод для фильтрации с пагинацией
    @Query("SELECT e FROM Employee e WHERE " +
            "(:name IS NULL OR :name = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:skill IS NULL OR :skill = '' OR :skill MEMBER OF e.skills) AND " +
            "(:departments IS NULL OR e.department IN :departments) AND " +
            "(:positions IS NULL OR e.position IN :positions) AND " +
            "(:active IS NULL OR e.active = :active) AND " +
            "e.deleted = :deleted")
    Page<Employee> findWithFilters(@Param("name") String name,
                                   @Param("skill") String skill,
                                   @Param("departments") List<String> departments,
                                   @Param("positions") List<String> positions,
                                   @Param("active") Boolean active,
                                   @Param("deleted") boolean deleted,
                                   Pageable pageable);

    // Метод для подсчета с фильтрами
    @Query("SELECT COUNT(e) FROM Employee e WHERE " +
            "(:name IS NULL OR :name = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:skill IS NULL OR :skill = '' OR :skill MEMBER OF e.skills) AND " +
            "(:departments IS NULL OR e.department IN :departments) AND " +
            "(:positions IS NULL OR e.position IN :positions) AND " +
            "(:active IS NULL OR e.active = :active) AND " +
            "e.deleted = :deleted")
    long countWithFilters(@Param("name") String name,
                          @Param("skill") String skill,
                          @Param("departments") List<String> departments,
                          @Param("positions") List<String> positions,
                          @Param("active") Boolean active,
                          @Param("deleted") boolean deleted);

    // Альтернативная версия для списка (без пагинации)
    @Query("SELECT e FROM Employee e WHERE " +
            "(:name IS NULL OR :name = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:category IS NULL OR :category = '' OR EXISTS (SELECT s FROM e.skills s WHERE LOWER(s) LIKE LOWER(CONCAT('%', :category, '%')))) AND " +
            "(:skill IS NULL OR :skill = '' OR :skill MEMBER OF e.skills) AND " +
            "(:departments IS NULL OR e.department IN :departments) AND " +
            "(:positions IS NULL OR e.position IN :positions) AND " +
            "(:active IS NULL OR e.active = :active)")
    List<Employee> findWithFiltersList(@Param("name") String name,
                                       @Param("category") String category,
                                       @Param("skill") String skill,
                                       @Param("departments") List<String> departments,
                                       @Param("positions") List<String> positions,
                                       @Param("active") Boolean active);

    List<Employee> findByDeletedTrue();

    Optional<Employee> findByIdAndDeletedTrue(Long id);

    Optional<Employee> findByIdAndDeletedFalse(Long id);

    long countByDeletedTrue();

    long countByDeletedFalse();

    boolean existsByIdAndDeletedFalse(Long id);

    long countByNameContainingAndDeletedFalse(String name);

    @Query("SELECT e FROM Employee e WHERE " +
            "e.deleted = true AND " +
            "(:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:departments IS NULL OR e.department IN :departments) AND " +
            "(:positions IS NULL OR e.position IN :positions)")
    Page<Employee> findDeletedWithFilters(@Param("name") String name,
                                          @Param("departments") List<String> departments,
                                          @Param("positions") List<String> positions,
                                          Pageable pageable);

}*/