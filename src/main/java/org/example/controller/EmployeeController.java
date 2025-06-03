package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Employee;
import org.example.service.EmployeeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    private static final String UPLOAD_DIR = "src/main/resources/static/images/";
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @GetMapping("/")
    public String listEmployees(@RequestParam(defaultValue = "0") int page, Model model) {
        int pageSize = 10;
        try {
            log.debug("Загрузка сотрудников для страницы: {}, размер страницы: {}", page, pageSize);
            List<Employee> employees = employeeService.findAll(page * pageSize, pageSize);
            long totalEmployees = employeeService.count();
            int totalPages = (int) Math.ceil((double) totalEmployees / pageSize);

            model.addAttribute("employees", employees);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
        } catch (IllegalArgumentException e) {
            log.error("Некорректные параметры для страницы {}: {}", page, e.getMessage());
            model.addAttribute("error", "Некорректные параметры");
        } catch (Exception e) {
            log.error("Ошибка при загрузке сотрудников для страницы {}: {}", page, e.getMessage());
            model.addAttribute("error", "Ошибка при загрузке списка сотрудников");
        }
        return "employees";
    }

    @GetMapping("/employee/new")
    public String newEmployee(Model model) {
        model.addAttribute("employee", new Employee());
        log.debug("Открыта форма для нового сотрудника");
        return "employee-edit";
    }

    @GetMapping({"/employee/add", "/employee/edit/{id}"})
    public String editEmployee(@PathVariable(required = false) Long id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            Employee employee;
            if (id != null) {
                log.debug("Запрос на редактирование сотрудника с ID: {}", id);
                employee = employeeService.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден с ID: " + id));
            } else {
                log.debug("Запрос на создание нового сотрудника");
                employee = new Employee();
            }
            model.addAttribute("employee", employee);
            return "employee-edit";
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка при открытии формы сотрудника с ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        } catch (Exception e) {
            log.error("Ошибка при открытии формы сотрудника с ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Произошла ошибка при загрузке формы");
            return "redirect:/";
        }
    }

    @GetMapping("/employee/{id}")
    public String viewEmployee(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            Employee employee = employeeService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден с ID: " + id));
            if (employee.getPhotoPath() != null) {
                Path filePath = Paths.get(UPLOAD_DIR, employee.getPhotoPath().replace("/images/", ""));
                if (!Files.exists(filePath)) {
                    employee.setPhotoPath("/images/default.jpg");
                }
            }
            model.addAttribute("employee", employee);
            return "employee-details";
        } catch (IllegalArgumentException e) {
            log.warn("Сотрудник с ID {} не найден", id);
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        } catch (Exception e) {
            log.error("Ошибка при загрузке сотрудника с ID {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "Ошибка при загрузке сотрудника");
            return "redirect:/";
        }
    }

    @PostMapping("/employee/save")
    public String saveEmployee(@Valid @ModelAttribute("employee") Employee employee,
                               BindingResult result,
                               @RequestParam(value = "photo", required = false) MultipartFile photo,
                               RedirectAttributes redirect) {
        if (result.hasErrors()) {
            log.debug("Ошибки валидации при сохранении сотрудника: {}", result.getAllErrors());
            return "employee-edit";
        }

        try {
            String photoPath = null;
            if (photo != null && !photo.isEmpty()) {
                photoPath = storeFile(photo);
            }

            Employee savedEmployee;
            if (employee.getId() == null) {
                log.info("Сохранение нового сотрудника: {}", employee.getName());
                savedEmployee = employeeService.save(employee, photoPath);
                redirect.addFlashAttribute("success", "Сотрудник успешно добавлен");
            } else {
                log.info("Обновление сотрудника с ID: {}", employee.getId());
                if (photoPath != null && employee.getPhotoPath() != null) {
                    deleteFile(employee.getPhotoPath());
                }
                savedEmployee = employeeService.update(employee.getId(), employee, photoPath);
                redirect.addFlashAttribute("success", "Сотрудник успешно обновлён");
            }
            return "redirect:/employee/" + savedEmployee.getId();
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка при сохранении сотрудника: {}", e.getMessage());
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/employee/" + (employee.getId() != null ? employee.getId() : "new");
        } catch (Exception e) {
            log.error("Ошибка при сохранении сотрудника: {}", e.getMessage());
            redirect.addFlashAttribute("error", "Ошибка при сохранении сотрудника");
            return "redirect:/employee/" + (employee.getId() != null ? employee.getId() : "new");
        }
    }

    @PostMapping("/employee/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            log.info("Удаление сотрудника с ID: {}", id);
            Employee employee = employeeService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден с ID: " + id));
            if (employee.getPhotoPath() != null) {
                deleteFile(employee.getPhotoPath());
            }
            employeeService.delete(id);
            redirect.addFlashAttribute("success", "Сотрудник успешно удалён");
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка при удалении сотрудника с ID {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        } catch (Exception e) {
            log.error("Ошибка при удалении сотрудника с ID {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "Ошибка при удалении сотрудника");
            return "redirect:/";
        }
    }

    private String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            log.debug("Пустой файл - сохранение не требуется");
            return null;
        }

        validateFile(file);

        String fileName = generateUniqueFileName(Objects.requireNonNull(file.getOriginalFilename()));
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
            throw e;
        }
    }

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

    private String generateUniqueFileName(String originalFilename) {
        String safeFileName = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        return UUID.randomUUID() + "_" + safeFileName;
    }
}