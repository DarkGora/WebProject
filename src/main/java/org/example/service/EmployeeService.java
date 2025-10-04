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
        return employeeRepository.findAllActivePaginated(offset, limit);
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

        // Обработка skill с безопасной обработкой ошибок
        String skillEnumName = null;
        if (skill != null && !skill.isBlank()) {
            try {
                Skills skillEnum = Skills.fromString(skill);
                if (skillEnum != null) {
                    skillEnumName = skillEnum.name();
                } else {
                    log.warn("Неизвестный навык: '{}'. Фильтрация по этому навыку будет пропущена.", skill);
                    // Возвращаем пустой список, так как фильтр по навыку не может быть применен
                    return List.of();
                }
            } catch (Exception e) {
                log.warn("Ошибка при обработке навыка '{}': {}. Фильтрация по навыку пропущена.",
                        skill, e.getMessage());
                return List.of();
            }
        }

        // Категория временно игнорируется, так как она вызывает проблемы в запросах
        if (category != null && !category.isBlank()) {
            log.info("Фильтрация по категории временно отключена: {}", category);
        }

        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Employee> page = employeeRepositoryJPA.findWithFilters(
                name, skillEnumName,
                departments, positions, active, false, pageable
        );

        return page.getContent();
    }
    @Transactional(readOnly = true)
    public long countWithFilters(String name, String category, String skill,
                                 List<String> departments, List<String> positions, Boolean active) {
        // Обработка skill для count метода
        String skillEnumName = null;
        if (skill != null && !skill.isBlank()) {
            Skills skillEnum = Skills.fromString(skill);
            if (skillEnum != null) {
                skillEnumName = skillEnum.name();
            } else {
                // Если навык неизвестен, возвращаем 0
                return 0;
            }
        }

        // Категория временно игнорируется
        if (category != null && !category.isBlank()) {
            log.info("Подсчет по категории временно отключен: {}", category);
        }

        return employeeRepositoryJPA.countWithFilters(name, skillEnumName,
                departments, positions, active, false);
    }

    // Обновите также метод countActiveWithFilters
    @Transactional(readOnly = true)
    public long countActiveWithFilters(String name, String category, String skill,
                                       List<String> departments, List<String> positions) {
        return countWithFilters(name, category, skill, departments, positions, true);
    }
// === НОВЫЕ МЕТОДЫ ДЛЯ РАБОТЫ С УДАЛЕННЫМИ СОТРУДНИКАМИ ===

    @Transactional(readOnly = true)
    public List<Employee> findDeletedEmployees() {
        log.debug("Поиск всех удаленных сотрудников");
        return employeeRepositoryJPA.findByDeletedTrue();
    }

    @Transactional(readOnly = true)
    public List<Employee> findDeletedEmployeesWithFilters(String name, List<String> departments,
                                                          List<String> positions) {
        log.debug("Фильтрация удаленных сотрудников: name={}, departments={}, positions={}",
                name, departments, positions);

        Pageable pageable = PageRequest.of(0, 100); // или нужная пагинация
        Page<Employee> page = employeeRepositoryJPA.findDeletedWithFilters(
                name, departments, positions, pageable
        );
        return page.getContent();
    }

    @Transactional(readOnly = true)
    public long countDeletedEmployees() {
        return employeeRepositoryJPA.countByDeletedTrue();
    }

    @Transactional(readOnly = true)
    public Optional<Employee> findDeletedById(Long id) {
        if (id == null) throw new IllegalArgumentException("ID не может быть null");
        return employeeRepositoryJPA.findByIdAndDeletedTrue(id);
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
        return employeeRepositoryJPA.findByIdAndDeletedFalse(id);
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

        if (employee.isDeleted()) {
            throw new IllegalArgumentException("Сотрудник уже удален");
        }

        employee.setDeleted(true);
        employee.setDeletedAt(LocalDateTime.now());
        employeeRepository.save(employee);
        log.info("Сотрудник ID {} перемещен в архив", id);
    }
    @Transactional
    public void restore(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));

        if (!employee.isDeleted()) {
            throw new IllegalArgumentException("Сотрудник не был удален");
        }

        employee.setDeleted(false);
        employee.setDeletedAt(null);
        employeeRepository.save(employee);
        log.info("Сотрудник ID {} восстановлен из архива", id);
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
        return employeeRepositoryJPA.existsByIdAndDeletedFalse(id);
    }

    @Transactional(readOnly = true)
    public long countByNameContaining(String name) {
        if (name == null || name.isBlank()) {
            return employeeRepositoryJPA.countByDeletedFalse();
        }
        return employeeRepositoryJPA.countByNameContainingAndDeletedFalse(name);
    }

    @Transactional(readOnly = true)
    public long countActiveEmployees() {
        return employeeRepositoryJPA.countByDeletedFalse();
    }

    @Transactional
    public void permanentDelete(Long id) {
        Employee employee = employeeRepositoryJPA.findByIdAndDeletedTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Удаленный сотрудник не найден"));

        // Сначала удаляем связанные записи
        educationRepository.deleteByEmployeeId(id);
        reviewRepository.deleteByEmployeeId(id);

        // Затем удаляем сотрудника
        employeeRepository.delete(employee);
        log.info("Сотрудник ID {} полностью удален из системы", id);
    }

    @Transactional(readOnly = true)
    public List<Employee> findByNameContaining(String name, int offset, int limit) {
        return employeeRepository.findActiveByNameContainingPaginated(name, offset, limit);
    }
}