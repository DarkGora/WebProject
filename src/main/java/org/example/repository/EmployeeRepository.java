package org.example.repository;

import org.example.model.Employee;
import org.example.model.Skills;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneNumberContaining(
            String name, String email, String phoneNumber);
    Optional<Employee> findByEmail(String email);

    // Базовые методы уже включены в JpaRepository:
    // findAll(), findById(), save(), deleteById(), delete(), existsById(), count()

    // Методы поиска
    List<Employee> findByNameContaining(String namePart);

    List<Employee> findBySkill(Skills skill);

    List<Employee> findBySkillsContaining(Skills skill);
    List<Employee> findByEmailContaining(String emailPart);

    List<Employee> findByNameContainingIgnoreCase(String namePart);
    List<Employee> findByEmailContainingIgnoreCase(String emailPart);
    List<Employee> findByPhoneNumberContaining(String phonePart);

    // Пагинация
    Page<Employee> findByNameContaining(String namePart, Pageable pageable);

    // Сложные запросы
    @Query("SELECT e FROM Employee e JOIN e.skills s WHERE e.school = :school AND s = :skill")
    List<Employee> findBySchoolAndSkill(@Param("school") String school, @Param("skill") Skills skill);

    List<Employee> findBySchoolAndSkills(String school, Skills skill);

    @Query("SELECT e FROM Employee e WHERE e.name LIKE %:name% OR e.email LIKE %:email%")
    List<Employee> findByNameOrEmail(@Param("name") String name, @Param("email") String email);

    <T> List<T> findBy(Class<T> type);

    // Проекции
    @Query("SELECT DISTINCT e.school FROM Employee e")
    List<String> findAllDistinctSchools();

    // Модифицирующие операции
    @Modifying
    @Query("UPDATE Employee e SET e.email = :newEmail WHERE e.id = :id")
    int updateEmailById(@Param("id") Long id, @Param("newEmail") String newEmail);

    @Modifying
    @Query("DELETE FROM Employee e WHERE e.school = :school")
    int deleteBySchool(@Param("school") String school);

    // Работа с коллекцией навыков
    @Query("SELECT DISTINCT e FROM Employee e JOIN e.skills s WHERE s IN :skills")
    List<Employee> findBySkillsIn(@Param("skills") List<Skills> skills);

    List<Employee> findByCategory(String category);

    // Если нужно добавить категории (предполагается, что нужно добавить поле category в Employee)
    // List<Employee> findByCategory(String category);
}