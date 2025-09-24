package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Employee;
import org.example.model.Review;
import org.example.model.Skills;
import org.example.service.EmployeeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    private static final String UPLOAD_DIR = "src/main/resources/static/images/";
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @GetMapping("/")
    public String listEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) List<String> department,
            @RequestParam(required = false) List<String> position,
            @RequestParam(required = false) Boolean active,
            Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();// авторизация
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        int pageSize = 10;
        try {
            log.debug("Загрузка сотрудников для страницы: {}, размер страницы: {}", page, pageSize);
            List<Employee> employees = employeeService.findWithFilters(
                    page * pageSize, pageSize, name, category, skill, department, position, active
            );
            long totalEmployees = employeeService.countWithFilters(name, category, skill, department, position, active);
            long activeEmployees = employeeService.countActiveWithFilters(name, category, skill, department, position);

            int totalPages = (int) Math.ceil((double) totalEmployees / pageSize);

            List<String> departments = employeeService.findAllDistinctDepartments();
            List<String> positions = employeeService.findAllDistinctPositions();

            model.addAttribute("employees", employees);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalEmployees", totalEmployees);
            model.addAttribute("activeEmployees", activeEmployees);
            model.addAttribute("departments", departments);
            model.addAttribute("positions", positions);
            model.addAttribute("skillCategories", Skills.getAllCategories());

            model.addAttribute("searchName", name);
            model.addAttribute("searchCategory", category);
            model.addAttribute("searchSkill", skill);
            model.addAttribute("searchDepartments", department);
            model.addAttribute("searchPositions", position);
            model.addAttribute("searchActive", active);

        } catch (IllegalArgumentException e) {
            log.error("Некорректные параметры для страницы {}: {}", page, e.getMessage());
            model.addAttribute("error", "Некорректные параметры");
        } catch (Exception e) {
            log.error("Ошибка при загрузке сотрудников для страницы {}: {}", page, e.getMessage(), e);
            model.addAttribute("error", "Ошибка при загрузке списка сотрудников");
        }
        return "employees";
    }
    @ModelAttribute("paginationParams")
    public String getPaginationParams(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) List<String> department,
            @RequestParam(required = false) List<String> position,
            @RequestParam(required = false) Boolean active) {

        StringBuilder params = new StringBuilder();

        if (name != null && !name.isBlank()) {
            params.append("&name=").append(URLEncoder.encode(name, StandardCharsets.UTF_8));
        }
        if (category != null && !category.isBlank()) {
            params.append("&category=").append(URLEncoder.encode(category, StandardCharsets.UTF_8));
        }
        if (skill != null && !skill.isBlank()) {
            params.append("&skill=").append(URLEncoder.encode(skill, StandardCharsets.UTF_8));
        }
        if (department != null && !department.isEmpty()) {
            department.forEach(dept -> params.append("&department=").append(URLEncoder.encode(dept, StandardCharsets.UTF_8)));
        }
        if (position != null && !position.isEmpty()) {
            position.forEach(pos -> params.append("&position=").append(URLEncoder.encode(pos, StandardCharsets.UTF_8)));
        }
        if (active != null) {
            params.append("&active=").append(active);
        }

        return params.toString();
    }
    @PreAuthorize("hasAuthority('ROLE_resume.admin')")
    @GetMapping("/employee/new")
    public String newEmployee(Model model) {
        model.addAttribute("employee", new Employee());
        log.debug("Открыта форма для нового сотрудника");
        return "employee-edit";
    }
    @PreAuthorize("hasAuthority('ROLE_resume.admin')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_resume.admin','ROLE_resume.client','ROLE_resume.user')")
    @GetMapping("/employee/{id}/reviews")
    public String viewEmployeeReviews(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        log.info("Запрос на просмотр отзывов для сотрудника ID: {}", id);
        try {
            Employee employee = employeeService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден с ID: " + id));
            List<Review> reviews = employeeService.findReviewsByEmployeeId(id);
            double averageRating = employeeService.calculateAverageRating(id);
            log.debug("Найдено {} отзывов для сотрудника ID: {}. Средний рейтинг: {}",
                    reviews.size(), id, averageRating);

            model.addAttribute("employee", employee);
            model.addAttribute("reviews", reviews);
            model.addAttribute("averageRating", String.format("%.2f", averageRating));
            return "employee-reviews";
        } catch (IllegalArgumentException e) {
            log.warn("Сотрудник с ID {} не найден", id);
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        } catch (Exception e) {
            log.error("Ошибка при загрузке отзывов для сотрудника ID {}: {}", id, e.getMessage(), e);
            redirect.addFlashAttribute("error", "Ошибка при загрузке отзывов");
            return "redirect:/employee/" + id;
        }
    }
    @PreAuthorize("hasAnyAuthority('ROLE_resume.admin','ROLE_resume.client','ROLE_resume.user')")
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
            Review review = new Review();
            review.setEmployee(employee);
            log.debug("Инициализация Review для формы: id={}, employee={}",
                    review.getId(), employee.getId());
            model.addAttribute("employee", employee);
            model.addAttribute("review", review);
            return "employee-details";
        } catch (IllegalArgumentException e) {
            log.warn("Сотрудник с ID {} не найден", id);
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        } catch (Exception e) {
            log.error("Ошибка при загрузке сотрудника с ID {}: {}", id, e.getMessage(), e);
            redirect.addFlashAttribute("error", "Ошибка при загрузке сотрудника");
            return "redirect:/";
        }
    }
    @PreAuthorize("hasAuthority('ROLE_resume.admin')")
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
    @PreAuthorize("hasAuthority('ROLE_resume.admin')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_resume.admin','ROLE_resume.client','ROLE_resume.user')")
    @GetMapping("/search")
    public String searchEmployees(@RequestParam(defaultValue = "") String name,
                                  @RequestParam(defaultValue = "0") int page,
                                  Model model) {
        int pageSize = 10;
        try {
            log.debug("Поиск сотрудников по имени: {}, страница: {}, размер: {}", name, page, pageSize);
            List<Employee> employees = employeeService.findByNameContaining(name, page * pageSize, pageSize);

            long totalFound = employeeService.countByNameContaining(name);
            int totalPages = (int) Math.ceil((double) totalFound / pageSize);

            model.addAttribute("employees", employees);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalEmployees", totalFound);
            model.addAttribute("searchName", name);
            model.addAttribute("skillCategories", Skills.getAllCategories());
            return "employees";
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при поиске сотрудников: имя={}, страница={}: {}", name, page, e.getMessage());
            model.addAttribute("error", "Ошибка при поиске сотрудников");
            return "employees";
        }
    }
    @PreAuthorize("hasAnyAuthority('ROLE_resume.admin','ROLE_resume.client','ROLE_resume.user')")
    @PostMapping("/employee/{id}/review")
    public String addReview(@PathVariable Long id,
                            @Valid @ModelAttribute("review") Review review,
                            BindingResult result,
                            RedirectAttributes redirect) {
        log.info("Попытка добавить отзыв для сотрудника ID: {}", id);

        if (!employeeService.existsById(id)) {
            log.warn("Сотрудник с ID {} не найден", id);
            redirect.addFlashAttribute("error", "Сотрудник не найден");
            return "redirect:/";
        }
        if (review.getEmployee() == null || !review.getEmployee().getId().equals(id)) {
            log.warn("Некорректная связь с сотрудником: получено {}, ожидалось {}",
                    review.getEmployee() != null ? review.getEmployee().getId() : "null", id);
            result.rejectValue("employee", "error.review", "Некорректный сотрудник");
        }

        if (result.hasErrors()) {
            log.warn("Ошибки валидации отзыва: {}", result.getAllErrors());
            redirect.addFlashAttribute("error", "Ошибка в данных отзыва: " +
                    result.getAllErrors().stream()
                            .map(e -> e.getDefaultMessage())
                            .collect(Collectors.joining(", ")));
            redirect.addFlashAttribute("org.springframework.validation.BindingResult.review", result);
            redirect.addFlashAttribute("review", review);
            return "redirect:/employee/" + id;
        }

        try {
            employeeService.saveReview(review);
            log.info("Отзыв успешно сохранен для сотрудника ID: {}", id);
            redirect.addFlashAttribute("success", "Отзыв успешно добавлен");
        } catch (Exception e) {
            log.error("Ошибка при сохранении отзыва для сотрудника ID {}: {}", id, e.getMessage(), e);
            redirect.addFlashAttribute("error", "Ошибка при добавлении отзыва: " + e.getMessage());
        }
        return "redirect:/employee/" + id;
    }
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Неверное имя пользователя или пароль");
        }
        if (logout != null) {
            model.addAttribute("message", "Вы успешно вышли из системы");
        }
        return "login";
    }
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
    @PostMapping("/logout")
    public String Logout(@AuthenticationPrincipal OidcUser oidcUser,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {

        if (oidcUser != null && oidcUser.getIdToken() != null) {
            String idToken = oidcUser.getIdToken().getTokenValue();
            String logoutUrl = "http://localhost:8081/realms/resume/protocol/openid-connect/logout";
            String redirectUri = "http://localhost:8080/login?logout=true";

            String fullLogoutUrl = logoutUrl +
                    "?id_token_hint=" + idToken +
                    "&post_logout_redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

            response.sendRedirect(fullLogoutUrl);
            return null;
        }

        request.getSession().invalidate();
        return "redirect:/login";
    }


    private String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            log.debug("Пустой файл - сохранение не требуется");
            return null;
        }
        try {
            validateFile(file);
            String fileName = generateUniqueFileName(Objects.requireNonNull(file.getOriginalFilename()));
            Path filePath = Paths.get(UPLOAD_DIR, fileName).toAbsolutePath().normalize();

            Files.createDirectories(filePath.getParent());
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Файл сохранен: {} в {}", fileName, filePath);
            return "/images/" + fileName;
        } catch (IllegalArgumentException e) {
            log.error("Ошибка валидации файла: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("Ошибка при сохранении файла: {}", e.getMessage());
            throw new IOException("Ошибка при сохранении файла: " + e.getMessage(), e);
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