package org.example;

import org.example.model.Employee;
import org.example.model.Skills;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {
    // Основные CRUD операции
    List<Employee> findAll();
    Optional<Employee> findById(Long id);
    Employee save(Employee employee);
    void deleteById(Long id);
    void delete(Employee employee);
    boolean existsById(Long id);

    List<Employee> findByNameContaining(String namePart);

    List<Employee> findBySkill(Skills skill);

    List<Employee> findByEmailContaining(String emailPart);

    long count();

    // Методы поиска
    List<Employee> findByNameContainingIgnoreCase(String namePart);
    List<Employee> findByEmailContainingIgnoreCase(String emailPart);
    List<Employee> findBySkillsContaining(Skills skill);
    List<Employee> findByPhoneNumberContaining(String phonePart);

    // Пагинация
    Page<Employee> findAll(Pageable pageable);
    Page<Employee> findByNameContaining(String namePart, Pageable pageable);

    // Сложные запросы
    List<Employee> findBySchoolAndSkills(String school, Skills skill);
    List<Employee> findByNameOrEmail(String name, String email);

    // Проекции
    <T> List<T> findBy(Class<T> type);
    List<String> findAllDistinctSchools();

    // Модифицирующие операции
    int updateEmailById(Long id, String newEmail);
    int deleteBySchool(String school);

    List<Employee> findBySkillsIn(List<Skills> skills);

    List<Employee> findByCategory(String category);
}