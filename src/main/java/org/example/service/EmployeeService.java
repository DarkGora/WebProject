package org.example.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Employee;
import org.example.model.Education;
import org.example.model.Review;
import org.example.repository.EmployeeRepository;
import org.example.repository.EducationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EducationRepository educationRepository;

    @Transactional(readOnly = true)
    public List<Employee> findAll(int offset, int limit) {
        if (offset < 0 || limit <= 0) {
            log.warn("Некорректные параметры пагинации: offset={}, limit={}", offset, limit);
            throw new IllegalArgumentException("Offset должен быть >= 0, limit > 0");
        }
        log.debug("Получение сотрудников с пагинацией: offset={}, limit={}", offset, limit);
        return employeeRepository.findAllPaginated(offset, limit);
    }

    @Transactional(readOnly = true)
    public List<Employee> findAllSorted(String sortField, boolean ascending) {
        validateSortField(sortField);
        log.debug("Получение сотрудников с сортировкой по: {} {}", sortField, ascending);
        return employeeRepository.findAllSorted(sortField, ascending);
    }
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        log.debug("Проверка существования сотрудника с ID: {}", id);
        return employeeRepository.existsById(id);
    }

    @Transactional
    public void saveReview(@NotNull @Valid Review review) {
        Objects.requireNonNull(review, "Отзыв не может быть null");
        log.info("Сохранение отзыва: id={}, employeeId={}, rating={}, comment={}",
                review.getId(), review.getEmployeeId(), review.getRating(), review.getComment());
        try {
            employeeRepository.saveReview(review);
            log.debug("Отзыв успешно передан в репозиторий");
        } catch (Exception e) {
            log.error("Ошибка при сохранении отзыва для сотрудника ID {}: {}",
                    review.getEmployeeId(), e.getMessage(), e);
            throw new RuntimeException("Не удалось сохранить отзыв", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Review> findReviewsByEmployeeId(@NotNull Long employeeId) {
        Objects.requireNonNull(employeeId, "ID сотрудника не может быть null");
        log.debug("Получение отзывов для сотрудника ID: {}", employeeId);
        return employeeRepository.findReviewsByEmployeeId(employeeId);
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(@NotNull Long employeeId) {
        Objects.requireNonNull(employeeId, "ID сотрудника не может быть null");
        log.debug("Получение среднего рейтинга для сотрудника ID: {}", employeeId);
        List<Review> reviews = findReviewsByEmployeeId(employeeId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);
    }


    @Transactional(readOnly = true)
    public Optional<Employee> findById(Long id) {
        if (id == null) {
            log.warn("Попытка найти сотрудника с null ID");
            throw new IllegalArgumentException("ID не может быть null");
        }
        log.debug("Получение сотрудника по ID: {}", id);
        return employeeRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Employee> findByNameContaining(String namePart, int offset, int limit) {
        if (offset < 0 || limit <= 0) {
            log.warn("Некорректные параметры пагинации: offset={}, limit={}", offset, limit);
            throw new IllegalArgumentException("Offset должен быть >= 0, limit > 0");
        }
        if (namePart == null || namePart.isBlank()) {
            log.debug("namePart пустой, возвращается пустой список");
            return Collections.emptyList();
        }
        log.debug("Получение сотрудников по имени, содержащему: {} с offset: {}, limit: {}",
                namePart, offset, limit);
        return employeeRepository.findByNameContainingPaginated(namePart, offset, limit);
    }

    @Transactional(readOnly = true)
    public long count() {
        log.debug("Подсчет сотрудников, прокси репозитория: {}",
                employeeRepository.getClass().getName());
        return employeeRepository.count();
    }

    @Transactional
    public Employee save(@NotNull @Valid Employee employee, String photoPath) {
        Objects.requireNonNull(employee, "Сотрудник не может быть null");
        log.info("Сохранение сотрудника: {}", employee.getName());

        if (photoPath != null && !photoPath.isBlank()) {
            employee.setPhotoPath(photoPath);
        }

        if (employee.getCreatedAt() == null) {
            employee.setCreatedAt(LocalDateTime.now());
        }

        // Инициализация списка educations, если он null
        if (employee.getEducations() == null) {
            employee.setEducations(new ArrayList<>());
        }

        // Устанавливаем связь employee для каждой записи Education
        for (Education education : employee.getEducations()) {
            education.setEmployee(employee);
        }

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Сотрудник сохранён с ID: {}", savedEmployee.getId());
        return savedEmployee;
    }

    @Transactional
    public Employee update(@NotNull Long id, @NotNull @Valid Employee employeeDetails, String photoPath) {
        Objects.requireNonNull(id, "ID не может быть null");
        Objects.requireNonNull(employeeDetails, "Данные сотрудника не могут быть null");
        log.info("Обновление сотрудника с ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден с ID: " + id));

        employee.setName(employeeDetails.getName());
        employee.setEmail(employeeDetails.getEmail());
        employee.setPhoneNumber(employeeDetails.getPhoneNumber());
        employee.setSchool(employeeDetails.getSchool());
        employee.setSkills(employeeDetails.getSkills());
        employee.setAbout(employeeDetails.getAbout());
        employee.setResume(employeeDetails.getResume());
        employee.setTelegram(employeeDetails.getTelegram());

        if (photoPath != null && !photoPath.isBlank()) {
            employee.setPhotoPath(photoPath);
        }

        // Обновляем educations
        employee.getEducations().clear(); // Очищаем старые записи
        if (employeeDetails.getEducations() != null && !employeeDetails.getEducations().isEmpty()) {
            for (Education education : employeeDetails.getEducations()) {
                education.setEmployee(employee);
                employee.getEducations().add(education);
            }
        }

        Employee updatedEmployee = employeeRepository.save(employee);
        log.info("Сотрудник обновлён с ID: {}", updatedEmployee.getId());
        return updatedEmployee;
    }

    @Transactional
    public void delete(@NotNull Long id) {
        Objects.requireNonNull(id, "ID не может быть null");
        log.info("Удаление сотрудника с ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден с ID: " + id));

        educationRepository.deleteByEmployeeId(id);
        employeeRepository.deleteById(id);
    }

    private void validateSortField(String sortField) {
        if (sortField == null || sortField.isBlank()) {
            return;
        }
        List<String> validFields = List.of("name", "email", "phonenumber", "school", "createdat");
        if (!validFields.contains(sortField.toLowerCase())) {
            log.warn("Некорректное поле сортировки: {}", sortField);
            throw new IllegalArgumentException("Недопустимое поле сортировки: " + sortField);
        }
    }
}