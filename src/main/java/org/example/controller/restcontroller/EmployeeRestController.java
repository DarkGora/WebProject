package org.example.controller.restcontroller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.dto.EmployeeQuickViewDTO;
import org.example.model.Employee;
import org.example.model.Education;
import org.example.model.Review;
import org.example.model.Skills;
import org.example.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Management", description = "API для управления сотрудниками")
public class EmployeeRestController {
    private final EmployeeService employeeService;

    private static final String UPLOAD_DIR = "resources/static/images/";
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB


    @PreAuthorize("hasAnyRole('resume.user', 'resume.admin', 'resume.client')")
    @Operation(summary = "Получить список сотрудников (с пагинацией и фильтрами)")
    @GetMapping
    public ResponseEntity<?> listEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String department) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Employee> employees = employeeService.findAllWithFilters(name, position, department, pageable);

            return ResponseEntity.ok()
                    .header("X-Total-Pages", String.valueOf(employees.getTotalPages()))
                    .header("X-Total-Count", String.valueOf(employees.getTotalElements()))
                    .body(employees.getContent());
        } catch (Exception e) {
            log.error("Ошибка при загрузке списка сотрудников: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка сервера при загрузке списка сотрудников");
        }
    }

    @PreAuthorize("hasAnyRole('resume.user', 'resume.admin', 'resume.client')")
    @Operation(summary = "Получить сотрудника по ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> getEmployee(@PathVariable Long id) {
        try {
            Employee employee = employeeService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Сотрудник с ID " + id + " не найден"));

            if (employee.getPhotoPath() != null && !employee.getPhotoPath().isEmpty()) {
                String cleanPhotoPath = employee.getPhotoPath().replaceFirst("^/", "");
                Path photoFullPath = Paths.get(UPLOAD_DIR, cleanPhotoPath).toAbsolutePath().normalize();

                if (!Files.exists(photoFullPath)) {
                    log.warn("Фото не найдено: {}", photoFullPath);
                    employee.setPhotoPath("/images/default-avatar.png");
                }
            } else {
                employee.setPhotoPath("/images/default-avatar.png");
            }

            return ResponseEntity.ok(employee);
        } catch (IllegalArgumentException e) {
            log.warn("Сотрудник не найден: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при получении сотрудника с ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка сервера при получении сотрудника");
        }
    }

    @PreAuthorize("hasAnyRole('resume.user', 'resume.admin', 'resume.client')")
    @Operation(summary = "Получить данные сотрудника для быстрого просмотра")
    @GetMapping("/{id}/quick-view")
    public ResponseEntity<?> getEmployeeQuickView(@PathVariable Long id) {
        try {
            Employee employee = employeeService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Сотрудник с ID " + id + " не найден"));

            String photoPath = "/images/default-avatar.png";
            if (employee.getPhotoPath() != null && !employee.getPhotoPath().isEmpty()) {
                String cleanPhotoPath = employee.getPhotoPath().replaceFirst("^/", "");
                Path photoFullPath = Paths.get(UPLOAD_DIR, cleanPhotoPath).toAbsolutePath().normalize();
                if (Files.exists(photoFullPath)) {
                    photoPath = employee.getPhotoPath();
                } else {
                    log.warn("Фото не найдено: {}", photoFullPath);
                }
            }

            EmployeeQuickViewDTO quickView = new EmployeeQuickViewDTO();
            quickView.setId(employee.getId());
            quickView.setName(employee.getName());
            quickView.setPosition(employee.getPosition());
            quickView.setDepartment(employee.getDepartment());
            quickView.setEmail(employee.getEmail());
            quickView.setPhoneNumber(employee.getPhoneNumber());
            quickView.setActive(employee.isActive());
            quickView.setCreatedAt(employee.getCreatedAt());
            quickView.setPhotoPath(photoPath);

            List<String> skillsAsStrings = employee.getSkills() != null && !employee.getSkills().isEmpty()
                    ? employee.getSkills().stream()
                    .map(Skills::getDisplayName)
                    .collect(Collectors.toList())
                    : List.of();

            quickView.setSkills(skillsAsStrings);

            return ResponseEntity.ok(quickView);
        } catch (IllegalArgumentException e) {
            log.warn("Сотрудник не найден для быстрого просмотра: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при получении данных сотрудника с ID {} для быстрого просмотра: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка сервера при получении данных для быстрого просмотра");
        }
    }

    // === АДМИНИСТРИРОВАНИЕ (только для resume.admin) ===

    @PreAuthorize("hasRole('resume.admin')")
    @Operation(summary = "Создать нового сотрудника")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createEmployee(
            @Valid @ModelAttribute Employee employee,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        try {
            String photoPath = null;
            if (photo != null && !photo.isEmpty()) {
                photoPath = storeFile(photo);
            }
            Employee savedEmployee = employeeService.save(employee, photoPath);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedEmployee);
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка валидации при создании сотрудника: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            log.warn("Ошибка загрузки файла: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Ошибка загрузки фото: " + e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при создании сотрудника: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка сервера при создании сотрудника");
        }
    }

    @PreAuthorize("hasRole('resume.admin')")
    @Operation(summary = "Обновить сотрудника")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateEmployee(
            @PathVariable Long id,
            @Valid @ModelAttribute Employee employee,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        try {
            String photoPath = null;
            if (photo != null && !photo.isEmpty()) {
                photoPath = storeFile(photo);
            }
            Employee updatedEmployee = employeeService.update(id, employee, photoPath);
            return ResponseEntity.ok(updatedEmployee);
        } catch (IllegalArgumentException e) {
            log.warn("Сотрудник не найден при обновлении: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            log.warn("Ошибка загрузки файла при обновлении: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Ошибка загрузки фото: " + e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при обновлении сотрудника с ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка сервера при обновлении сотрудника");
        }
    }

    @PreAuthorize("hasRole('resume.admin')")
    @Operation(summary = "Удалить сотрудника")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        try {
            employeeService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Сотрудник не найден при удалении: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Ошибка при удалении сотрудника с ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка сервера при удалении сотрудника");
        }
    }

    // === Skills (только для admin) ===

    @PreAuthorize("hasRole('resume.admin')")
    @Operation(summary = "Добавить навык сотруднику")
    @PostMapping("/{id}/skills")
    public ResponseEntity<?> addSkill(
            @PathVariable Long id,
            @RequestParam String skill) {
        try {
            employeeService.addSkill(id, skill.trim());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка при добавлении навыка: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при добавлении навыка сотруднику с ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка сервера при добавлении навыка");
        }
    }

    @PreAuthorize("hasRole('resume.admin')")
    @Operation(summary = "Удалить навык у сотрудника")
    @DeleteMapping("/{id}/skills/{skill}")
    public ResponseEntity<?> removeSkill(
            @PathVariable Long id,
            @PathVariable String skill) {
        try {
            employeeService.removeSkill(id, skill);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Навык не найден при удалении: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Ошибка при удалении навыка у сотрудника с ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка сервера при удалении навыка");
        }
    }

    // === Educations (только для admin) ===

    @PreAuthorize("hasRole('resume.admin')")
    @Operation(summary = "Добавить образование сотруднику")
    @PostMapping("/{id}/educations")
    public ResponseEntity<?> addEducation(
            @PathVariable Long id,
            @Valid @RequestBody Education education) {
        try {
            employeeService.addEducation(id, education);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка при добавлении образования: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при добавлении образования сотруднику с ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка сервера при добавлении образования");
        }
    }

    // === Reviews (для всех аутентифицированных) ===

    @PreAuthorize("hasAnyRole('resume.user', 'resume.admin', 'resume.client')")
    @Operation(summary = "Добавить отзыв о сотруднике")
    @PostMapping("/{id}/reviews")
    public ResponseEntity<?> addReview(
            @PathVariable Long id,
            @Valid @RequestBody Review review) {
        try {
            Employee employee = employeeService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));
            review.setEmployee(employee);
            employeeService.saveReview(review);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка при добавлении отзыва: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при добавлении отзыва для сотрудника с ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка сервера при добавлении отзыва");
        }
    }

    // === Utils ===

    private String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID() + fileExtension;

        Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(fileName);

        Files.createDirectories(uploadPath);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/images/" + fileName;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не должен быть пустым");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Недопустимый формат файла. Разрешены: " + ALLOWED_IMAGE_TYPES);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Файл слишком большой. Максимальный размер: " + (MAX_FILE_SIZE / (1024 * 1024)) + "MB");
        }
    }
}