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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final FileStorageService fileStorageService;

    /**
     * Получение списка сотрудников с пагинацией.
     *
     * @param offset начальная позиция (неотрицательная)
     * @param limit  количество записей (положительное)
     * @return список сотрудников
     * @throws IllegalArgumentException если offset < 0 или limit <= 0
     */
    @Transactional(readOnly = true)
    public List<Employee> findAll(int offset, int limit) {
        if (offset < 0 || limit <= 0) {
            log.warn("Некорректные параметры пагинации: offset={}, limit={}", offset, limit);
            throw new IllegalArgumentException("Offset должен быть >= 0, limit > 0");
        }
        log.debug("Получение сотрудников с пагинацией: offset={}, limit={}", offset, limit);
        return employeeRepository.findAllPaginated(offset, limit);
    }

    /**
     * Получение списка сотрудников с сортировкой.
     *
     * @param sortField поле для сортировки (name, email, phonenumber, school, createdat)
     * @param ascending направление сортировки (true - по возрастанию, false - по убыванию)
     * @return список сотрудников
     * @throws IllegalArgumentException если sortField некорректное
     */
    @Transactional(readOnly = true)
    public List<Employee> findAllSorted(String sortField, boolean ascending) {
        validateSortField(sortField);
        log.debug("Получение сотрудников с сортировкой по: {} {}", sortField, ascending);
        return employeeRepository.findAllSorted(sortField, ascending);
    }

    /**
     * Поиск сотрудника по ID.
     *
     * @param id идентификатор сотрудника (не null)
     * @return Optional с сотрудником или пустой, если не найден
     * @throws IllegalArgumentException если id null
     */
    @Transactional(readOnly = true)
    public Optional<Employee> findById(Long id) {
        if (id == null) {
            log.warn("Попытка найти сотрудника с null ID");
            throw new IllegalArgumentException("ID не может быть null");
        }
        log.debug("Получение сотрудника по ID: {}", id);
        return employeeRepository.findById(id);
    }

    /**
     * Поиск сотрудников по части имени с пагинацией.
     *
     * @param namePart подстрока для поиска в имени
     * @param offset   начальная позиция (неотрицательная)
     * @param limit    количество записей (положительное)
     * @return список сотрудников или пустой список, если namePart null или пустой
     * @throws IllegalArgumentException если offset < 0 или limit <= 0
     */
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

    /**
     * Подсчет общего количества сотрудников.
     *
     * @return количество сотрудников
     */
    @Transactional(readOnly = true)
    public long count() {
        log.debug("Подсчет сотрудников, прокси репозитория: {}",
                employeeRepository.getClass().getName());
        return employeeRepository.count();
    }

    /**
     * Сохранение нового сотрудника с обработкой фото и образований.
     *
     * @param employee сотрудник для сохранения (не null)
     * @param photo    файл фотографии (может быть null)
     * @return сохраненный сотрудник
     * @throws IllegalArgumentException если employee null
     * @throws RuntimeException        если произошла ошибка при сохранении файла
     */
    @Transactional
    public Employee save(@NotNull @Valid Employee employee, MultipartFile photo) {
        Objects.requireNonNull(employee, "Сотрудник не может быть null");
        log.info("Сохранение сотрудника: {}", employee.getName());

        try {
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
        } catch (IOException e) {
            log.error("Ошибка при сохранении файла для сотрудника: {}", employee.getName(), e);
            throw new RuntimeException("Не удалось сохранить фотографию сотрудника", e);
        }
    }

    /**
     * Обновление существующего сотрудника.
     *
     * @param id              идентификатор сотрудника (не null)
     * @param employeeDetails данные для обновления (не null)
     * @param photo           файл фотографии (может быть null)
     * @return обновленный сотрудник
     * @throws IllegalArgumentException если id или employeeDetails null, или сотрудник не найден
     * @throws RuntimeException        если произошла ошибка при сохранении файла
     */
    @Transactional
    public Employee update(@NotNull Long id, @NotNull @Valid Employee employeeDetails, MultipartFile photo) {
        Objects.requireNonNull(id, "ID не может быть null");
        Objects.requireNonNull(employeeDetails, "Данные сотрудника не могут быть null");
        log.info("Обновление сотрудника с ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден с ID: " + id));

        try {
            // Обновляем поля
            employee.setName(employeeDetails.getName());
            employee.setEmail(employeeDetails.getEmail());
            employee.setPhoneNumber(employeeDetails.getPhoneNumber());
            employee.setSchool(employeeDetails.getSchool());
            employee.setSkills(employeeDetails.getSkills());

            // Обработка фото
            if (photo != null && !photo.isEmpty()) {
                if (employee.getPhotoPath() != null) {
                    fileStorageService.deleteFile(employee.getPhotoPath());
                }
                employee.setPhotoPath(fileStorageService.storeFile(photo));
            }

            // Обновление образований
            updateEducations(employee, employeeDetails.getEducations());

            return employeeRepository.save(employee);
        } catch (IOException e) {
            log.error("Ошибка при обновлении файла для сотрудника с ID: {}", id, e);
            throw new RuntimeException("Не удалось обновить фотографию сотрудника", e);
        }
    }

    /**
     * Удаление сотрудника и связанного фото.
     *
     * @param id идентификатор сотрудника (не null)
     * @throws IllegalArgumentException если id null или сотрудник не найден
     */
    @Transactional
    public void delete(@NotNull Long id) {
        Objects.requireNonNull(id, "ID не может быть null");
        log.info("Удаление сотрудника с ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден с ID: " + id));

        if (employee.getPhotoPath() != null) {
            try {
                fileStorageService.deleteFile(employee.getPhotoPath());
            } catch (IOException e) {
                log.warn("Не удалось удалить файл: {}", employee.getPhotoPath(), e);
            }
        }

        educationRepository.deleteByEmployeeId(id);
        employeeRepository.deleteById(id);
    }

    /**
     * Сохранение списка образований с привязкой к сотруднику.
     *
     * @param educations список образований
     * @param employee   сотрудник
     */
    private void saveEducations(List<Education> educations, Employee employee) {
        if (educations == null) {
            return;
        }
        educations.forEach(education -> {
            education.setEmployee(employee);
            educationRepository.save(education);
        });
    }

    /**
     * Обновление списка образований сотрудника.
     *
     * @param employee   сотрудник
     * @param educations новый список образований
     */
    private void updateEducations(Employee employee, List<Education> educations) {
        educationRepository.deleteByEmployeeId(employee.getId());
        if (educations != null && !educations.isEmpty()) {
            saveEducations(educations, employee);
        }
    }

    /**
     * Валидация поля сортировки.
     *
     * @param sortField поле сортировки
     * @throws IllegalArgumentException если поле некорректное
     */
    private void validateSortField(String sortField) {
        if (sortField == null || sortField.isBlank()) {
            return; // Репозиторий обработает null как findAll
        }
        List<String> validFields = List.of("name", "email", "phonenumber", "school", "createdat");
        if (!validFields.contains(sortField.toLowerCase())) {
            log.warn("Некорректное поле сортировки: {}", sortField);
            throw new IllegalArgumentException("Недопустимое поле сортировки: " + sortField);
        }
    }
}