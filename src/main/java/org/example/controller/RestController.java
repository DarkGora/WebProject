package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Employee;
import org.example.service.EmployeeService;
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
@org.springframework.web.bind.annotation.RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class RestController {
    private final EmployeeService employeeService;

    private static final String UPLOAD_DIR = "src/main/resources/static/images/";
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    // Получение списка сотрудников с пагинацией
    @GetMapping
    public ResponseEntity<?> listEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.debug("Запрос списка сотрудников: страница={}, размер={}", page, size);
            List<Employee> employees = employeeService.findAll(page * size, size);
            long totalEmployees = employeeService.count();
            int totalPages = (int) Math.ceil((double) totalEmployees / size);

            return ResponseEntity.ok()
                    .header("X-Total-Pages", String.valueOf(totalPages))
                    .header("X-Total-Count", String.valueOf(totalEmployees))
                    .body(employees);
        } catch (IllegalArgumentException e) {
            log.error("Некорректные параметры пагинации: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Некорректные параметры: " + e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при загрузке списка сотрудников: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка сервера при загрузке списка сотрудников");
        }
    }

    // Получение сотрудника по ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getEmployee(@PathVariable Long id) {
        try {
            log.debug("Запрос сотрудника с ID: {}", id);
            Employee employee = employeeService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден с ID: " + id));

            // Проверка существования фото
            if (employee.getPhotoPath() != null) {
                Path filePath = Paths.get(UPLOAD_DIR, employee.getPhotoPath().replace("/images/", ""));
                if (!Files.exists(filePath)) {
                    employee.setPhotoPath("/images/default.jpg");
                }
            }
            return ResponseEntity.ok(employee);
        } catch (IllegalArgumentException e) {
            log.warn("Сотрудник с ID {} не найден: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при загрузке сотрудника с ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка сервера при загрузке сотрудника");
        }
    }

    // Создание нового сотрудника
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createEmployee(
            @Valid @ModelAttribute Employee employee,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        try {
            log.info("Создание нового сотрудника: {}", employee.getName());
            String photoPath = storeFile(photo);
            Employee savedEmployee = employeeService.save(employee, photoPath);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedEmployee);
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка при создании сотрудника: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка сервера при создании сотрудника: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка сервера при создании сотрудника");
        }
    }

    // Обновление существующего сотрудника
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateEmployee(
            @PathVariable Long id,
            @Valid @ModelAttribute Employee employee,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        try {
            log.info("Обновление сотрудника с ID: {}", id);
            String photoPath = storeFile(photo);
            Employee updatedEmployee = employeeService.update(id, employee, photoPath);
            return ResponseEntity.ok(updatedEmployee);
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка при обновлении сотрудника с ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка сервера при обновлении сотрудника с ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка сервера при обновлении сотрудника");
        }
    }

    // Удаление сотрудника
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        try {
            log.info("Удаление сотрудника с ID: {}", id);
            Employee employee = employeeService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден с ID: " + id));
            if (employee.getPhotoPath() != null && !employee.getPhotoPath().equals("/images/default.jpg")) {
                deleteFile(employee.getPhotoPath());
            }
            employeeService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка при удалении сотрудника с ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка сервера при удалении сотрудника с ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка сервера при удалении сотрудника");
        }
    }

    // Сохранение файла изображения
    private String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            log.debug("Пустой файл - сохранение не требуется");
            return null;
        }

        validateFile(file);

        String fileName = generateUniqueFileName(file.getOriginalFilename());
        Path filePath = Paths.get(UPLOAD_DIR, fileName).toAbsolutePath().normalize();

        try {
            Files.createDirectories(filePath.getParent());
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Файл сохранен: {} в {}", fileName, filePath);
            return "/images/" + fileName;
        } catch (IOException e) {
            log.error("Ошибка при сохранении файла {}: {}", fileName, e.getMessage());
            throw new IOException("Не удалось сохранить файл", e);
        }
    }

    // Удаление файла изображения
    private void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            log.debug("Не указано имя файла для удаления");
            return;
        }

        try {
            Path path = Paths.get(UPLOAD_DIR, filePath.replace("/images/", "")).toAbsolutePath().normalize();
            Files.deleteIfExists(path);
            log.info("Файл удален: {}", filePath);
        } catch (IOException e) {
            log.warn("Не удалось удалить файл: {}", filePath, e);
        }
    }

    // Валидация файла
    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            log.error("Недопустимый тип файла: {}", contentType);
            throw new IllegalArgumentException("Поддерживаются только файлы JPG, PNG и WEBP");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.error("Превышен размер файла: {} байт", file.getSize());
            throw new IllegalArgumentException("Максимальный размер файла - 5MB");
        }
    }

    // Генерация уникального имени файла
    private String generateUniqueFileName(String originalFilename) {
        if (originalFilename == null) {
            return UUID.randomUUID().toString();
        }
        String safeFileName = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        return UUID.randomUUID() + "_" + safeFileName;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hi!";
    }

    @GetMapping("/names")
    public String getfindByNameOrEmail() {
        Employee employee = employeeService.findById(8L).get();
        return employee.toString();
    }
}