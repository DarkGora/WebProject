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
import java.util.stream.Collectors;

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

    // === НОВЫЕ МЕТОДЫ ДЛЯ ФИЛЬТРАЦИИ ===

    @Transactional(readOnly = true)
    public List<Employee> findWithFilters(int offset, int limit, String name, String category,
                                          String skill, List<String> departments,
                                          List<String> positions, Boolean active) {
        log.debug("Фильтры: name={}, category={}, skill={}, departments={}, positions={}, active={}",
                name, category, skill, departments, positions, active);

        if (offset < 0 || limit <= 0) {
            throw new IllegalArgumentException("Offset должен быть >= 0, limit > 0");
        }

        // Преобразуем skill из строки в enum, если нужно
        Skills skillEnum = null;
        if (skill != null && !skill.isBlank()) {
            skillEnum = Skills.fromString(skill);
            if (skillEnum == null) {
                log.warn("Неизвестный навык: {}", skill);
                // Возвращаем пустой список или обрабатываем иначе
                return List.of();
            }
        }

        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Employee> page = employeeRepositoryJPA.findWithFilters(
                name, category, skillEnum != null ? skillEnum.name() : null,
                departments, positions, active, pageable
        );

        return page.getContent();
    }

    @Transactional(readOnly = true)
    public long countWithFilters(String name, String category, String skill,
                                 List<String> departments, List<String> positions, Boolean active) {
        return employeeRepositoryJPA.countWithFilters(name, category, skill, departments, positions, active);
    }

    @Transactional(readOnly = true)
    public long countActiveWithFilters(String name, String category, String skill,
                                       List<String> departments, List<String> positions) {
        return employeeRepositoryJPA.countWithFilters(name, category, skill, departments, positions, true);
    }

    @Transactional(readOnly = true)
    public List<String> findAllDistinctDepartments() {
        return employeeRepositoryJPA.findDistinctDepartments();
    }

    @Transactional(readOnly = true)
    public List<String> findAllDistinctPositions() {
        return employeeRepositoryJPA.findDistinctPositions();
    }

    // === СУЩЕСТВУЮЩИЕ МЕТОДЫ ===

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

    // === Навыки (Skills) - ИСПРАВЛЕННЫЕ МЕТОДЫ ===

    @Transactional
    public void addSkill(Long employeeId, String skillName) {
        if (employeeId == null) throw new IllegalArgumentException("ID сотрудника не может быть null");
        if (skillName == null || skillName.isBlank()) throw new IllegalArgumentException("Название навыка не может быть пустым");

        Skills skill = Skills.fromString(skillName);
        if (skill == null) {
            throw new IllegalArgumentException("Неизвестный навык: " + skillName);
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));

        // Используем метод addSkill из сущности Employee
        employee.addSkill(skill);
        employeeRepository.save(employee);
    }

    @Transactional
    public void removeSkill(Long employeeId, String skillName) {
        Skills skill = Skills.fromString(skillName);
        if (skill == null) {
            throw new IllegalArgumentException("Неизвестный навык: " + skillName);
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));

        if (!employee.getSkills().contains(skill)) {
            throw new IllegalArgumentException("У сотрудника нет навыка: " + skillName);
        }

        // Используем метод removeSkill из сущности Employee
        employee.removeSkill(skill);
        employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public Set<String> getSkills(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));

        // Преобразуем Skills в строки (displayName)
        return employee.getSkills().stream()
                .map(Skills::getDisplayName)
                .collect(Collectors.toSet());
    }

    // Альтернативный метод для получения Skills как enum'ов
    @Transactional(readOnly = true)
    public Set<Skills> getSkillsAsEnum(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));
        return employee.getSkills();
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
        if (review.getEmployee() == null || review.getEmployee().getId() == null) {
            throw new IllegalArgumentException("Сотрудник не может быть null");
        }

        Employee employee = employeeRepository.findById(review.getEmployee().getId())
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
    public long countByNameContaining(String name) {
        if (name == null || name.isBlank()) {
            return employeeRepository.count();
        }
        return employeeRepository.countByNameContaining(name);
    }

    @Transactional(readOnly = true)
    public List<Employee> findByNameContaining(String name, int offset, int limit) {
        return employeeRepository.findByNameContainingPaginated(name, offset, limit);
    }
}