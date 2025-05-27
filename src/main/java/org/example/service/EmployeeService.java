package org.example.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Employee;
import org.example.model.Education;
import org.example.repository.EmployeeRepository;
import org.example.repository.EducationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

        Employee savedEmployee = employeeRepository.save(employee);

        if (employee.getEducations() != null && !employee.getEducations().isEmpty()) {
            saveEducations(employee.getEducations(), savedEmployee);
        }

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

        if (photoPath != null && !photoPath.isBlank()) {
            employee.setPhotoPath(photoPath);
        }

        updateEducations(employee, employeeDetails.getEducations());

        return employeeRepository.save(employee);
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

    private void saveEducations(List<Education> educations, Employee employee) {
        if (educations == null) {
            return;
        }
        educations.forEach(education -> {
            education.setEmployee(employee);
            educationRepository.save(education);
        });
    }

    private void updateEducations(Employee employee, List<Education> educations) {
        educationRepository.deleteByEmployeeId(employee.getId());
        if (educations != null && !educations.isEmpty()) {
            saveEducations(educations, employee);
        }
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