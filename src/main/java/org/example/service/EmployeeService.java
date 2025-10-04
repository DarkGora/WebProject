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

    // === ОСНОВНЫЕ ОПЕРАЦИИ СОХРАНЕНИЯ ===

    @Transactional
    public Employee create(@NotNull @Valid Employee employee, String photoPath) {
        log.info("Создание нового сотрудника: {}", employee.getName());

        Objects.requireNonNull(employee, "Сотрудник не может быть null");

        // Установка фото если есть
        if (photoPath != null) {
            employee.setPhotoPath(photoPath);
        }

        // Установка даты создания
        if (employee.getCreatedAt() == null) {
            employee.setCreatedAt(LocalDateTime.now());
        }

        // Сброс флага удаления при создании
        employee.setDeleted(false);
        employee.setDeletedAt(null);
        employee.setDeletedBy(null);

        Employee saved = employeeRepository.save(employee);
        log.debug("Создан сотрудник ID: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Employee update(@NotNull @Valid Employee employee) {
        log.info("Обновление сотрудника ID: {}", employee.getId());

        Objects.requireNonNull(employee, "Сотрудник не может быть null");
        Objects.requireNonNull(employee.getId(), "ID сотрудника не может быть null");

        // Проверяем существование
        if (!employeeRepository.existsById(employee.getId())) {
            throw new IllegalArgumentException("Сотрудник с ID " + employee.getId() + " не найден");
        }

        Employee saved = employeeRepository.save(employee);
        log.debug("Обновлен сотрудник ID: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Employee updateWithPhoto(Long id, @NotNull @Valid Employee employeeDetails, String photoPath) {
        log.info("Обновление сотрудника ID: {} с фото", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));

        // Обновляем поля
        employee.setName(employeeDetails.getName());
        employee.setEmail(employeeDetails.getEmail());
        employee.setPosition(employeeDetails.getPosition());
        employee.setDepartment(employeeDetails.getDepartment());
        employee.setPhoneNumber(employeeDetails.getPhoneNumber());
        employee.setActive(employeeDetails.isActive());
        employee.setSchool(employeeDetails.getSchool());
        employee.setAbout(employeeDetails.getAbout());
        employee.setResume(employeeDetails.getResume());
        employee.setTelegram(employeeDetails.getTelegram());

        // Обновляем навыки если переданы
        if (employeeDetails.getSkills() != null) {
            employee.setSkills(employeeDetails.getSkills());
        }

        // Обновляем фото если передано новое
        if (photoPath != null) {
            employee.setPhotoPath(photoPath);
        }

        Employee saved = employeeRepository.save(employee);
        log.debug("Обновлен сотрудник ID: {} с фото", saved.getId());
        return saved;
    }

    // === СПЕЦИАЛЬНЫЕ ОПЕРАЦИИ ===

    @Transactional
    public Employee softDelete(Long id, String deletedBy) {
        log.info("Мягкое удаление сотрудника ID: {}, пользователем: {}", id, deletedBy);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));

        if (employee.isDeleted()) {
            throw new IllegalArgumentException("Сотрудник уже удален");
        }

        employee.softDelete(deletedBy);
        Employee saved = employeeRepository.save(employee);
        log.debug("Сотрудник ID: {} перемещен в архив", id);
        return saved;
    }

    @Transactional
    public Employee restore(Long id) {
        log.info("Восстановление сотрудника ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));

        if (!employee.isDeleted()) {
            throw new IllegalArgumentException("Сотрудник не был удален");
        }

        employee.setDeleted(false);
        employee.setDeletedAt(null);
        employee.setDeletedBy(null);

        Employee saved = employeeRepository.save(employee);
        log.debug("Сотрудник ID: {} восстановлен из архива", id);
        return saved;
    }

    // === DEPRECATED - для обратной совместимости ===

    @Deprecated
    @Transactional
    public Employee save(@NotNull @Valid Employee employee, String photoPath) {
        if (employee.getId() == null) {
            return create(employee, photoPath);
        } else {
            if (photoPath != null) {
                return updateWithPhoto(employee.getId(), employee, photoPath);
            } else {
                return update(employee);
            }
        }
    }

    // === МЕТОДЫ ПОИСКА И ФИЛЬТРАЦИИ ===

    @Transactional(readOnly = true)
    public List<Employee> findAll(int offset, int limit) {
        if (offset < 0 || limit <= 0) {
            log.warn("Некорректные параметры пагинации: offset={}, limit={}", offset, limit);
            throw new IllegalArgumentException("Offset должен быть >= 0, limit > 0");
        }
        Pageable pageable = PageRequest.of(offset / limit, limit);
        return employeeRepository.findAllActivePaginated(pageable);
    }

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
                    return List.of();
                }
            } catch (Exception e) {
                log.warn("Ошибка при обработке навыка '{}': {}. Фильтрация по навыку пропущена.",
                        skill, e.getMessage());
                return List.of();
            }
        }

        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Employee> page = employeeRepository.findWithFilters(
                name, skillEnumName,
                departments, positions, active, false, pageable
        );

        return page.getContent();
    }

    // НОВЫЙ МЕТОД ДЛЯ КОНТРОЛЛЕРА
    @Transactional(readOnly = true)
    public Page<Employee> findWithFilters(String name, String category, String skill,
                                          List<String> departments, List<String> positions,
                                          Boolean active, Pageable pageable) {
        log.debug("Фильтры с Pageable: name={}, category={}, skill={}, departments={}, positions={}, active={}",
                name, category, skill, departments, positions, active);

        // Обработка skill с безопасной обработкой ошибок
        String skillEnumName = null;
        if (skill != null && !skill.isBlank()) {
            try {
                Skills skillEnum = Skills.fromString(skill);
                if (skillEnum != null) {
                    skillEnumName = skillEnum.name();
                } else {
                    log.warn("Неизвестный навык: '{}'. Фильтрация по этому навыку будет пропущена.", skill);
                    return Page.empty();
                }
            } catch (Exception e) {
                log.warn("Ошибка при обработке навыка '{}': {}. Фильтрация по навыку пропущена.",
                        skill, e.getMessage());
                return Page.empty();
            }
        }

        return employeeRepository.findWithFilters(
                name, skillEnumName, departments, positions, active, false, pageable
        );
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
                return 0;
            }
        }

        return employeeRepository.countWithFilters(name, skillEnumName,
                departments, positions, active, false);
    }

    @Transactional(readOnly = true)
    public long countActiveWithFilters(String name, String category, String skill,
                                       List<String> departments, List<String> positions) {
        return countWithFilters(name, category, skill, departments, positions, true);
    }

    // === МЕТОДЫ ДЛЯ РАБОТЫ С УДАЛЕННЫМИ СОТРУДНИКАМИ ===

    @Transactional(readOnly = true)
    public List<Employee> findDeletedEmployees() {
        log.debug("Поиск всех удаленных сотрудников");
        return employeeRepository.findByDeletedTrue();
    }

    @Transactional(readOnly = true)
    public List<Employee> findDeletedEmployeesWithFilters(String name, List<String> departments,
                                                          List<String> positions) {
        log.debug("Фильтрация удаленных сотрудников: name={}, departments={}, positions={}",
                name, departments, positions);

        Pageable pageable = PageRequest.of(0, 100);
        Page<Employee> page = employeeRepository.findDeletedWithFilters(
                name, departments, positions, pageable
        );
        return page.getContent();
    }

    @Transactional(readOnly = true)
    public long countDeletedEmployees() {
        return employeeRepository.countByDeletedTrue();
    }

    @Transactional(readOnly = true)
    public Optional<Employee> findDeletedById(Long id) {
        if (id == null) throw new IllegalArgumentException("ID не может быть null");
        return employeeRepository.findByIdAndDeletedTrue(id);
    }

    @Transactional(readOnly = true)
    public List<String> findAllDistinctDepartments() {
        return employeeRepository.findDistinctDepartments();
    }

    @Transactional(readOnly = true)
    public List<String> findAllDistinctPositions() {
        return employeeRepository.findDistinctPositions();
    }

    // === ОСНОВНЫЕ МЕТОДЫ ПОИСКА ===

    @Transactional(readOnly = true)
    public Page<Employee> findAllWithFilters(String name, String position, String department, Pageable pageable) {
        return employeeRepository.findByNameContainingAndPositionAndDepartment(
                name != null ? name : "",
                position != null ? position : "",
                department != null ? department : "",
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Optional<Employee> findById(Long id) {
        if (id == null) throw new IllegalArgumentException("ID не может быть null");
        return employeeRepository.findByIdAndDeletedFalse(id);
    }

    @Transactional(readOnly = true)
    public List<Employee> findByNameContaining(String name, int offset, int limit) {
        if (offset < 0 || limit <= 0) {
            throw new IllegalArgumentException("Offset должен быть >= 0, limit > 0");
        }
        Pageable pageable = PageRequest.of(offset / limit, limit);
        return employeeRepository.findActiveByNameContainingPaginated(name, pageable);
    }

    // НОВЫЙ МЕТОД ДЛЯ КОНТРОЛЛЕРА
    @Transactional(readOnly = true)
    public Page<Employee> findByNameContainingPage(String name, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return employeeRepository.findByDeletedFalse(pageable);
        }
        return employeeRepository.findByNameContainingAndDeletedFalse(name, pageable);
    }

    // НОВЫЙ МЕТОД ДЛЯ КОНТРОЛЛЕРА
    @Transactional(readOnly = true)
    public Page<Employee> findAllActive(Pageable pageable) {
        return employeeRepository.findByDeletedFalse(pageable);
    }

    // === НАВЫКИ (Skills) ===

    @Transactional
    public void addSkill(Long employeeId, String skillName) {
        if (employeeId == null) throw new IllegalArgumentException("ID сотрудника не может быть null");
        if (skillName == null || skillName.isBlank()) throw new IllegalArgumentException("Название навыка не может быть пустым");

        try {
            log.info("=== НАЧАЛО ДОБАВЛЕНИЯ НАВЫКА ===");
            log.info("Параметры: employeeId={}, skillName='{}'", employeeId, skillName);

            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));

            log.info("Найден сотрудник: {} (ID: {})", employee.getName(), employee.getId());
            log.info("Текущие навыки ДО добавления: {}",
                    employee.getSkills() != null ?
                            employee.getSkills().stream()
                                    .map(s -> s.name() + " (" + s.getDisplayName() + ")")
                                    .collect(Collectors.toList()) : "null");

            // Поиск навыка
            Skills skill = Skills.fromString(skillName);
            log.info("Результат поиска навыка '{}': {}", skillName, skill);

            if (skill == null) {
                String availableSkills = Arrays.stream(Skills.values())
                        .map(s -> s.name() + "=" + s.getDisplayName())
                        .collect(Collectors.joining(", "));
                log.warn("Неизвестный навык: '{}'. Доступные: {}", skillName, availableSkills);
                throw new IllegalArgumentException("Неизвестный навык: " + skillName);
            }

            // Инициализация коллекции
            if (employee.getSkills() == null) {
                employee.setSkills(new HashSet<>());
                log.info("Инициализирована пустая коллекция навыков");
            }

            // Проверка дублирования
            if (employee.getSkills().contains(skill)) {
                log.info("Навык '{}' уже есть у сотрудника. Пропускаем добавление.", skill.getDisplayName());
                return;
            }

            // Добавление навыка
            log.info("Добавляем навык: {} ({})", skill.name(), skill.getDisplayName());
            employee.getSkills().add(skill);

            // Сохранение
            Employee saved = employeeRepository.save(employee);
            log.info("Сотрудник сохранен. ID: {}", saved.getId());

            // Проверка результата
            log.info("Навыки ПОСЛЕ добавления: {}",
                    saved.getSkills().stream()
                            .map(s -> s.name() + " (" + s.getDisplayName() + ")")
                            .collect(Collectors.toList()));

            log.info("=== УСПЕШНОЕ ДОБАВЛЕНИЕ НАВЫКА ===");

        } catch (Exception e) {
            log.error("=== ОШИБКА ПРИ ДОБАВЛЕНИИ НАВЫКА ===");
            log.error("employeeId: {}, skillName: '{}'", employeeId, skillName);
            log.error("Ошибка: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось добавить навык: " + e.getMessage(), e);
        }
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

        employee.removeSkill(skill);
        employeeRepository.save(employee);
        log.debug("Удален навык '{}' у сотрудника ID: {}", skillName, employeeId);
    }

    @Transactional(readOnly = true)
    public Set<String> getSkills(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));

        return employee.getSkills().stream()
                .map(Skills::getDisplayName)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<Skills> getSkillsAsEnum(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));
        return employee.getSkills();
    }

    // === ОБРАЗОВАНИЕ (Educations) ===

    @Transactional
    public void addEducation(Long employeeId, Education education) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));
        education.setEmployee(employee);
        educationRepository.save(education);
        log.debug("Добавлено образование сотруднику ID: {}", employeeId);
    }

    @Transactional(readOnly = true)
    public List<Education> getEducations(Long employeeId) {
        return educationRepository.findByEmployeeId(employeeId);
    }

    // === ОТЗЫВЫ (Reviews) ===

    @Transactional
    public Review saveReview(Review review) {
        if (review.getEmployee() == null || review.getEmployee().getId() == null) {
            throw new IllegalArgumentException("Сотрудник не может быть null");
        }

        Employee employee = employeeRepository.findById(review.getEmployee().getId())
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));

        review.setEmployee(employee);
        Review saved = reviewRepository.save(review);
        log.debug("Сохранен отзыв для сотрудника ID: {}", employee.getId());
        return saved;
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

    // === ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ===

    @Transactional(readOnly = true)
    public long count() {
        return employeeRepository.count();
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return employeeRepository.existsByIdAndDeletedFalse(id);
    }

    @Transactional(readOnly = true)
    public long countByNameContaining(String name) {
        if (name == null || name.isBlank()) {
            return employeeRepository.countByDeletedFalse();
        }
        return employeeRepository.countByNameContainingAndDeletedFalse(name);
    }

    @Transactional(readOnly = true)
    public long countActiveEmployees() {
        return employeeRepository.countByDeletedFalse();
    }

    @Transactional
    public void permanentDelete(Long id) {
        Employee employee = employeeRepository.findByIdAndDeletedTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Удаленный сотрудник не найден"));

        // Сначала удаляем связанные записи
        educationRepository.deleteByEmployeeId(id);
        reviewRepository.deleteByEmployeeId(id);

        // Затем удаляем сотрудника
        employeeRepository.delete(employee);
        log.info("Сотрудник ID {} полностью удален из системы", id);
    }

    // === УСТАРЕВШИЕ МЕТОДЫ ===

    @Deprecated
    @Transactional
    public Employee update(Long id, @NotNull @Valid Employee employeeDetails, String photoPath) {
        return updateWithPhoto(id, employeeDetails, photoPath);
    }

    @Deprecated
    @Transactional
    public void delete(Long id) {
        softDelete(id, "system");
    }
}