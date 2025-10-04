package org.example.repository;

import org.example.model.Employee;
import org.example.model.Review;
import org.example.model.Skills;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {
    List<Employee> findAll();
    List<Employee> findAllSorted(String sortField, boolean ascending);
    List<Employee> findAllPaginated(int offset, int limit);
    long count();
    Optional<Employee> findById(Long id);
    Optional<Employee> findByEmail(String email);
    Employee save(Employee employee);
    <S extends Employee> List<S> saveAll(Iterable<S> employees);
    void deleteById(Long id);
    void delete(Employee employee);
    void deleteAllById(Iterable<? extends Long> ids);
    void deleteAll(Iterable<? extends Employee> employees);
    void deleteAll();
    boolean existsById(Long id);
    List<Employee> findByNameContaining(String namePart);
    List<Employee> findByNameContainingPaginated(String namePart, int offset, int limit);
    List<Employee> findBySkill(Skills skill);
    List<Employee> findBySkills(List<Skills> skills);
    List<Employee> findBySkillCategory(String category);
    List<Employee> findByEmailContaining(String emailPart);
    List<Employee> findByPhoneNumberContaining(String phonePart);
    List<Employee> findBySchoolAndSkill(String school, Skills skill);
    List<Employee> findByNameOrEmail(String name, String email);
    List<String> findAllDistinctSchools();
    int updateEmailById(Long id, String newEmail);
    int deleteBySchool(String school);
    void saveReview(Review review);
    List<Review> findReviewsByEmployeeId(Long employeeId);
    long countByNameContaining(String name);
    List<Employee> findByDeletedTrue();
    List<Employee> findActiveByNameContainingPaginated(String name, int offset, int limit);
    List<Employee> findAllActivePaginated(int offset, int limit);
}