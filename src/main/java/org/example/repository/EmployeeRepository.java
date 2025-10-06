package org.example.repository;

import org.example.model.Employee;
import org.example.model.Review;
import org.example.model.Skills;
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

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // === ОСНОВНЫЕ МЕТОДЫ ПОИСКА ===
    Optional<Employee> findByIdAndDeletedFalse(Long id);
    Optional<Employee> findByIdAndDeletedTrue(Long id);
    List<Employee> findByDeletedFalse();
    List<Employee> findByDeletedTrue();
    Page<Employee> findByDeletedFalse(Pageable pageable);
    Page<Employee> findByDeletedTrue(Pageable pageable);

    Optional<Employee> findByEmailAndDeletedFalse(String email);
    // === ПАГИНАЦИЯ ===
    @Query("SELECT e FROM Employee e WHERE e.deleted = false ORDER BY e.id")
    List<Employee> findAllActivePaginated(Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE " +
            "(:name IS NULL OR :name = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "e.deleted = false ORDER BY e.name ASC")
    List<Employee> findActiveByNameContainingPaginated(@Param("name") String name,
                                                       Pageable pageable);

    // === ПОИСК С ФИЛЬТРАЦИЕЙ ===
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

    // === МЕТОДЫ ДЛЯ НАВЫКОВ ===
    @Query("SELECT e FROM Employee e JOIN e.skills s WHERE s = :skill AND e.deleted = false")
    List<Employee> findBySkill(@Param("skill") Skills skill);

    @Query("SELECT e FROM Employee e WHERE :skill MEMBER OF e.skills AND e.deleted = false")
    List<Employee> findBySkillContains(@Param("skill") Skills skill);

    @Modifying
    @Transactional
    @Query("UPDATE Employee e SET e.skills = :skills WHERE e.id = :id")
    void updateSkills(@Param("id") Long id, @Param("skills") Set<Skills> skills);

    // === МЕТОДЫ ДЛЯ СТАТИСТИКИ И АГРЕГАЦИИ ===
    long countByDeletedFalse();
    long countByDeletedTrue();
    long countByNameContainingAndDeletedFalse(String name);
    boolean existsByIdAndDeletedFalse(Long id);

    // === ПОИСК ПО РАЗЛИЧНЫМ ПОЛЯМ ===
    List<Employee> findByNameContainingAndDeletedFalse(String name);
    Page<Employee> findByNameContainingAndDeletedFalse(String name, Pageable pageable);
    List<Employee> findByEmailContainingAndDeletedFalse(String email);
    List<Employee> findByDepartmentAndDeletedFalse(String department);

    // === УТИЛИТНЫЕ МЕТОДЫ ===
    @Query("SELECT DISTINCT e.department FROM Employee e WHERE e.department IS NOT NULL AND e.deleted = false ORDER BY e.department")
    List<String> findDistinctDepartments();

    @Query("SELECT DISTINCT e.position FROM Employee e WHERE e.position IS NOT NULL AND e.deleted = false ORDER BY e.position")
    List<String> findDistinctPositions();

    // === МЕТОДЫ ДЛЯ УДАЛЕННЫХ СОТРУДНИКОВ ===
    @Query("SELECT e FROM Employee e WHERE " +
            "e.deleted = true AND " +
            "(:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:departments IS NULL OR e.department IN :departments) AND " +
            "(:positions IS NULL OR e.position IN :positions)")
    Page<Employee> findDeletedWithFilters(@Param("name") String name,
                                          @Param("departments") List<String> departments,
                                          @Param("positions") List<String> positions,
                                          Pageable pageable);

    // === NATIVE QUERIES ДЛЯ СЛОЖНЫХ ОПЕРАЦИЙ ===
    @Modifying
    @Transactional
    @Query(value = "UPDATE employees SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = :id",
            nativeQuery = true)
    void softDelete(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE employees SET deleted = false, deleted_at = NULL WHERE id = :id",
            nativeQuery = true)
    void restore(@Param("id") Long id);

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
    Optional<Employee> findByEmail(String email);
    Page<Employee> findByNameContaining(String name, Pageable pageable);

    long countByNameContaining(String name);

    @Query("SELECT e FROM Employee e JOIN e.skills s WHERE s IN :skillNames")
    List<Employee> findBySkillsIn(@Param("skillNames") List<String> skillNames);

    // === МЕТОД ДЛЯ ПОДСЧЕТА С ФИЛЬТРАМИ ===
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


}