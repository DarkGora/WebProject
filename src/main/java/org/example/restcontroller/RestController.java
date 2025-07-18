package org.example.restcontroller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.*;
import org.example.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Management", description = "API для управления сотрудниками")
class EmployeeRestController {
    private final EmployeeService employeeService;

    private static final String UPLOAD_DIR = "src/main/resources/static/images/";
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    // === Employee CRUD ===

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
            log.error("Ошибка при загрузке списка сотрудников: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Ошибка сервера");
        }
    }

    @Operation(summary = "Получить сотрудника по ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> getEmployee(@PathVariable Long id) {
        try {
            Employee employee = employeeService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));

            if (employee.getPhotoPath() != null && !Files.exists(Paths.get(UPLOAD_DIR + employee.getPhotoPath()))) {
                employee.setPhotoPath("/images/default.jpg");
            }

            return ResponseEntity.ok(employee);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ошибка сервера");
        }
    }

    @Operation(summary = "Создать нового сотрудника")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createEmployee(
            @Valid @ModelAttribute Employee employee,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        try {
            String photoPath = storeFile(photo);
            Employee savedEmployee = employeeService.save(employee, photoPath);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedEmployee);
        } catch (IllegalArgumentException | IOException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ошибка сервера");
        }
    }

    @Operation(summary = "Обновить сотрудника")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateEmployee(
            @PathVariable Long id,
            @Valid @ModelAttribute Employee employee,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        try {
            String photoPath = storeFile(photo);
            Employee updatedEmployee = employeeService.update(id, employee, photoPath);
            return ResponseEntity.ok(updatedEmployee);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Ошибка загрузки фото");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ошибка сервера");
        }
    }

    @Operation(summary = "Удалить сотрудника")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        try {
            employeeService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ошибка сервера");
        }
    }

    // === Employee Skills ===

    @Operation(summary = "Добавить навык сотруднику")
    @PostMapping("/{id}/skills")
    public ResponseEntity<?> addSkill(
            @PathVariable Long id,
            @RequestParam String skill) {
        try {
            employeeService.addSkill(id, skill);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ошибка сервера");
        }
    }

    @Operation(summary = "Удалить навык у сотрудника")
    @DeleteMapping("/{id}/skills/{skill}")
    public ResponseEntity<?> removeSkill(
            @PathVariable Long id,
            @PathVariable String skill) {
        try {
            employeeService.removeSkill(id, skill);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ошибка сервера");
        }
    }

    // === Educations ===

    @Operation(summary = "Добавить образование сотруднику")
    @PostMapping("/{id}/educations")
    public ResponseEntity<?> addEducation(
            @PathVariable Long id,
            @RequestBody Education education) {
        try {
            employeeService.addEducation(id, education);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ошибка сервера");
        }
    }

    // === Reviews ===

    @Operation(summary = "Добавить отзыв о сотруднике")
    @PostMapping("/{id}/reviews")
    public ResponseEntity<?> addReview(
            @PathVariable Long id,
            @RequestBody Review review) {
        try {
            employeeService.saveReview(review);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ошибка сервера");
        }
    }

    // === Utils ===

    private String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        validateFile(file);

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR, fileName).toAbsolutePath().normalize();

        Files.createDirectories(filePath.getParent());
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/images/" + fileName;
    }

    private void validateFile(MultipartFile file) {
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Недопустимый формат файла");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Файл слишком большой (макс. 5MB)");
        }
    }
}