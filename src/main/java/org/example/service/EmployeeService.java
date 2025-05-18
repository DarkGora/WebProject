package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Employee;
import org.example.model.Education;
import org.example.repository.EmployeeRepository;
import org.example.repository.EducationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional // Транзакция на уровне класса
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EducationRepository educationRepository;
    private final FileStorageService fileStorageService;
    private final TransactionTemplate transactionTemplate;

    /**
     * Получение списка сотрудников с пагинацией.
     * Использует TransactionTemplate для управления транзакцией.
     */
    public List<Employee> findAll(int offset, int limit) {
        return transactionTemplate.execute(status -> employeeRepository.findAllPaginated(offset, limit));
    }

    /**
     * Получение списка сотрудников с сортировкой.
     * Только для чтения.
     */
    @Transactional(readOnly = true)
    public List<Employee> findAllSorted(String sortField, boolean ascending) {
        log.debug("Получение сотрудников с сортировкой по: {} {}", sortField, ascending);
        return employeeRepository.findAllSorted(sortField, ascending);
    }

    /**
     * Поиск сотрудника по ID.
     */
    @Transactional(readOnly = true)
    public Optional<Employee> findById(Long id) {
        if (id == null) {
            log.warn("Попытка найти сотрудника с null ID");
            return Optional.empty();
        }
        log.debug("Получение сотрудника по ID: {}", id);
        return employeeRepository.findById(id);
    }

    /**
     * Поиск сотрудников по части имени с пагинацией.
     */
    @Transactional(readOnly = true)
    public List<Employee> findByNameContaining(String namePart, int offset, int limit) {
        log.debug("Получение сотрудников по имени, содержащему: {} с offset: {}, limit: {}",
                namePart, offset, limit);
        return employeeRepository.findByNameContainingPaginated(namePart, offset, limit);
    }

    /**
     * Подсчёт общего количества сотрудников.
     */
    @Transactional(readOnly = true)
    public long count() {
        log.debug("Подсчёт сотрудников, прокси репозитория: {}",
                employeeRepository.getClass().getName());
        return employeeRepository.count();
    }

    /**
     * Сохранение нового сотрудника с обработкой фото и образований.
     */
    public Employee save(Employee employee, MultipartFile photo) throws IOException {
        Objects.requireNonNull(employee, "Сотрудник не может быть null");
        log.info("Сохранение сотрудника: {}", employee.getName());

        if (photo != null && !photo.isEmpty()) {
            employee.setPhotoPath(fileStorageService.storeFile(photo));
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

    /**
     * Обновление существующего сотрудника.
     */
    public Employee update(Long id, Employee employeeDetails, MultipartFile photo) throws IOException {
        Objects.requireNonNull(id, "ID не может быть null");
        Objects.requireNonNull(employeeDetails, "Данные сотрудника не могут быть null");
        log.info("Обновление сотрудника с ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден с ID: " + id));

        // Обновляем поля
        employee.setName(employeeDetails.getName());
        employee.setEmail(employeeDetails.getEmail());
        employee.setSkills(employeeDetails.getSkills());
        employee.setCreatedAt(LocalDateTime.now());

        // Обработка фото
        if (photo != null && !photo.isEmpty()) {
            if (employee.getPhotoPath() != null) {
                fileStorageService.deleteFile(employee.getPhotoPath());
            }
            employee.setPhotoPath(fileStorageService.storeFile(photo));
        }

        // Обновление образований
        educationRepository.deleteByEmployeeId(id);
        if (employeeDetails.getEducations() != null && !employeeDetails.getEducations().isEmpty()) {
            saveEducations(employeeDetails.getEducations(), employee);
        }

        return employeeRepository.save(employee);
    }

    /**
     * Удаление сотрудника и связанного фото.
     */
    public void delete(Long id) {
        Objects.requireNonNull(id, "ID не может быть null");
        log.info("Удаление сотрудника с ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден с ID: " + id));

        if (employee.getPhotoPath() != null) {
            fileStorageService.deleteFile(employee.getPhotoPath());
        }

        educationRepository.deleteByEmployeeId(id);
        employeeRepository.deleteById(id);
    }

    /**
     * Сохранение списка образований с привязкой к сотруднику.
     */
    private void saveEducations(List<Education> educations, Employee employee) {
        educations.forEach(education -> {
            education.setEmployee(employee);
            educationRepository.save(education);
        });
    }
}
