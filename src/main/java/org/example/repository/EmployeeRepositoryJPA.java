package org.example.repository;

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
    // Для пагинации и фильтрации
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

    // Добавление навыка сотруднику
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO employee_skills (employee_id, skill) VALUES (:employeeId, :skill)",
            nativeQuery = true)
    void addSkillToEmployee(@Param("employeeId") Long employeeId,
                            @Param("skill") String skill);

    // Удаление навыка у сотрудника
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM employee_skills WHERE employee_id = :employeeId AND skill = :skill",
            nativeQuery = true)
    void removeSkillFromEmployee(@Param("employeeId") Long employeeId,
                                 @Param("skill") String skill);

    // Получение всех навыков сотрудника
    @Query(value = "SELECT skill FROM employee_skills WHERE employee_id = :employeeId",
            nativeQuery = true)
    Set<String> findSkillsByEmployeeId(@Param("employeeId") Long employeeId);

    // Проверка наличия навыка у сотрудника
    @Query(value = "SELECT COUNT(*) > 0 FROM employee_skills WHERE employee_id = :employeeId AND skill = :skill",
            nativeQuery = true)
    boolean hasSkill(@Param("employeeId") Long employeeId,
                     @Param("skill") String skill);

    Optional<Employee> findByName(String name);
    Optional<Employee> findByEmail(String name);
    Page<Employee> findByNameContaining(String name, Pageable pageable);

    long countByNameContaining(String name);

    @Query("SELECT e FROM Employee e JOIN e.skills s WHERE s IN :skillNames")
    List<Employee> findBySkillsIn(@Param("skillNames") List<String> skillNames);
}
