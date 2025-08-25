package org.example.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.*;
import org.example.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EducationRepository educationRepository;
    private final ReviewRepository reviewRepository;
    private final EmployeeRepositoryJPA employeeRepositoryJPA;

    @Transactional(readOnly = true)
    public List<Employee> findAll(int offset, int limit) {
        if (offset < 0 || limit <= 0) {
            log.warn("Некорректные параметры пагинации: offset={}, limit={}", offset, limit);
            throw new IllegalArgumentException("Offset должен быть >= 0, limit > 0");
        }
        return employeeRepository.findAllPaginated(offset, limit);
    }

    @Transactional(readOnly = true)
    public Page<Employee> findAllWithFilters(String name, String position, String department, Pageable pageable) {
        return employeeRepositoryJPA.findByNameContainingAndPositionAndDepartment(
                name != null ? name : "",
                position != null ? position : "",
                department != null ? department : "",
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Optional<Employee> findById(Long id) {
        if (id == null) throw new IllegalArgumentException("ID не может быть null");
        return employeeRepository.findById(id);
    }

    @Transactional
    public Employee save(@NotNull @Valid Employee employee, String photoPath) {
        Objects.requireNonNull(employee, "Сотрудник не может быть null");
        if (photoPath != null) employee.setPhotoPath(photoPath);
        if (employee.getCreatedAt() == null) employee.setCreatedAt(LocalDateTime.now());
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee update(Long id, @NotNull @Valid Employee employeeDetails, String photoPath) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));

        employee.setName(employeeDetails.getName());
        employee.setEmail(employeeDetails.getEmail());
        employee.setPosition(employeeDetails.getPosition());
        employee.setDepartment(employeeDetails.getDepartment());
        if (photoPath != null) employee.setPhotoPath(photoPath);

        return employeeRepository.save(employee);
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));
        employeeRepository.delete(employee);
    }

    // === Навыки (Skills) ===

    @Transactional
    public void addSkill(Long employeeId, String skillName) {
        Skills skill = Skills.fromString(skillName);
        if (skill == null) {
            throw new IllegalArgumentException("Неизвестный навык: " + skillName);
        }

        if (!employeeRepositoryJPA.hasSkill(employeeId, skill.name())) {
            employeeRepositoryJPA.addSkillToEmployee(employeeId, skill.name());
        }
    }

    @Transactional
    public void removeSkill(Long employeeId, String skillName) {
        Skills skill = Skills.fromString(skillName);
        if (skill == null) {
            throw new IllegalArgumentException("Неизвестный навык: " + skillName);
        }

        if (!employeeRepositoryJPA.hasSkill(employeeId, skill.name())) {
            throw new IllegalArgumentException("У сотрудника нет навыка: " + skillName);
        }

        employeeRepositoryJPA.removeSkillFromEmployee(employeeId, skill.name());
    }

    @Transactional(readOnly = true)
    public Set<String> getSkills(Long employeeId) {
        return employeeRepositoryJPA.findSkillsByEmployeeId(employeeId);
    }

    // === Образование (Educations) ===

    @Transactional
    public void addEducation(Long employeeId, Education education) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));
        education.setEmployee(employee);
        educationRepository.save(education);
    }

    @Transactional(readOnly = true)
    public List<Education> getEducations(Long employeeId) {
        return educationRepository.findByEmployeeId(employeeId);
    }

    // === Отзывы (Reviews) ===

    @Transactional
    public Review saveReview(Review review) {
        if (review.getEmployeeId() == null) {
            throw new IllegalArgumentException("ID сотрудника не может быть null");
        }
        Employee employee = employeeRepository.findById(review.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));
        review.setEmployee(employee);
        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<Review> findReviewsByEmployeeId(Long employeeId) {
        return reviewRepository.findByEmployeeId(employeeId);
    }

    @Transactional(readOnly = true)
    public double calculateAverageRating(Long employeeId) {
        Double avgRating = reviewRepository.calculateAverageRating(employeeId);
        return avgRating != null ? avgRating : 0.0;
    }

    // === Дополнительные методы ===

    @Transactional(readOnly = true)
    public long count() {
        return employeeRepository.count();
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return employeeRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public List<Employee> findByNameContaining(String name, int offset, int limit) {
        return employeeRepository.findByNameContainingPaginated(name, offset, limit);
    }
}