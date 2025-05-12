package org.example;


import lombok.extern.slf4j.Slf4j;
import org.example.model.Education;
import org.example.model.Employee;
import org.example.model.Skills;
import org.example.repository.EducationRepository;
import org.example.repository.EmployeeRepository;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@SpringBootApplication
public class Main {
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private final EmployeeRepository employeeRepository;
    private final EducationRepository educationRepository;
    private final String uploadDir;

    @Autowired
    public Main(EmployeeRepository employeeRepository, EducationRepository educationRepository,
                @Value("${upload.dir:uploads}") String uploadDir) {
        this.employeeRepository = employeeRepository;
        this.educationRepository = educationRepository;
        this.uploadDir = uploadDir;
        initializeUploadDirectory();
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    private void initializeUploadDirectory() {
        try {
            Path path = Paths.get(uploadDir);
            Files.createDirectories(path);
            log.info("Upload directory initialized: {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to initialize upload directory: {}", uploadDir, e);
            throw new RuntimeException("Cannot initialize upload directory", e);
        }
    }

    @GetMapping("/")
    public String showEmployees(Model model, @RequestParam(required = false) String search,
                                @RequestParam(required = false) String categoryFilter) {
        MDC.put("requestPath", "/");
        try {
            log.info("Search employees with query: {}, category: {}", search, categoryFilter);
            List<Employee> employees;
            if (search != null && !search.isBlank()) {
                search = search.trim();
                employees = employeeRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneNumberContaining(
                        search, search, search);
            } else {
                employees = employeeRepository.findAll();
            }
            if (categoryFilter != null && !categoryFilter.isBlank()) {
                employees = employees.stream()
                        .filter(emp -> categoryFilter.equals(emp.getCategory()))
                        .collect(Collectors.toList());
            }
            List<Employee> sortedEmployees = new ArrayList<>(employees);
            sortedEmployees.sort((a, b) -> {
                if ("Вася Пупкин".equals(a.getName())) return -1;
                if ("Вася Пупкин".equals(b.getName())) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            Optional<Employee> vasya = sortedEmployees.stream()
                    .filter(emp -> "Вася Пупкин".equals(emp.getName()))
                    .findFirst();
            if (vasya.isPresent() && sortedEmployees.size() > 1) {
                sortedEmployees.remove(vasya.get());
                sortedEmployees.add(1, vasya.get());
            }
            model.addAttribute("employees", sortedEmployees);
            model.addAttribute("categories", Skills.getAllCategories());
            model.addAttribute("allSkills", Skills.values());
            return "employees";
        } catch (Exception e) {
            log.error("Error fetching employees", e);
            model.addAttribute("errorMessage", "Не удалось загрузить список сотрудников. Пожалуйста, попробуйте позже.");
            return "employees";
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/employee/{id}")
    public String showEmployee(@PathVariable Long id, Model model) {
        MDC.put("employeeId", id.toString());
        try {
            Employee employee = employeeRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Неверный ID сотрудника: " + id));
            List<Education> educations = educationRepository.findByEmployeeId(id);
            model.addAttribute("employee", employee);
            model.addAttribute("educations", educations);
            return "employee-details";
        } catch (Exception e) {
            log.error("Error fetching employee", e);
            model.addAttribute("errorMessage", "Сотрудник не найден.");
            return "employees";
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/employee/edit/{id}")
    public String showEditForm(@PathVariable(required = false) String id, Model model) {
        MDC.put("employeeId", id);
        try {
            if (id == null || "null".equalsIgnoreCase(id)) {
                return "redirect:/";
            }
            Long employeeId = Long.parseLong(id);
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Неверный ID сотрудника: " + id));
            List<Education> educations = educationRepository.findByEmployeeId(employeeId);
            model.addAttribute("employee", employee);
            model.addAttribute("educations", educations);
            model.addAttribute("allSkills", Skills.values());
            model.addAttribute("categories", Skills.getAllCategories());
            return "employee-edit";
        } catch (NumberFormatException e) {
            log.warn("Invalid ID format: {}", id);
            return "redirect:/";
        } catch (Exception e) {
            log.error("Error fetching employee for edit", e);
            model.addAttribute("errorMessage", "Сотрудник не найден.");
            return "employees";
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/employee/new")
    public String showNewEmployeeForm(Model model) {
        model.addAttribute("employee", new Employee());
        model.addAttribute("educations", new ArrayList<Education>());
        model.addAttribute("allSkills", Skills.values());
        model.addAttribute("categories", Skills.getAllCategories());
        return "employee-edit";
    }

    @PostMapping("/employee/save")
    public String saveEmployee(
            @Valid @ModelAttribute Employee employee,
            BindingResult bindingResult,
            @RequestParam(value = "skills", required = false) List<String> skillNames,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @ModelAttribute("educations") List<Education> educations,
            RedirectAttributes redirectAttributes,
            Model model) {
        MDC.put("employeeEmail", employee.getEmail());
        try {
            if (bindingResult.hasErrors()) {
                model.addAttribute("employee", employee);
                model.addAttribute("educations", educations);
                model.addAttribute("allSkills", Skills.values());
                model.addAttribute("categories", Skills.getAllCategories());
                model.addAttribute("errorMessage", "Пожалуйста, исправьте ошибки в форме.");
                return "employee-edit";
            }
            List<Skills> skills = processSkills(skillNames);
            employee.setSkills(skills);
            if (photo != null && !photo.isEmpty()) {
                if (!isValidImage(photo)) {
                    model.addAttribute("employee", employee);
                    model.addAttribute("educations", educations);
                    model.addAttribute("allSkills", Skills.values());
                    model.addAttribute("categories", Skills.getAllCategories());
                    model.addAttribute("errorMessage", "Недопустимый тип или размер файла. Разрешены JPEG, PNG, GIF до 5MB.");
                    return "employee-edit";
                }
                String filename = saveUploadedFile(photo);
                employee.setPhotoPath("/" + uploadDir + "/" + filename);
                log.info("Uploaded photo: {}", filename);
            }
            if (employee.getCreatedAt() == null) {
                employee.setCreatedAt(LocalDateTime.now());
            }
            Employee savedEmployee = employeeRepository.save(employee);
            saveEducations(educations, savedEmployee.getId());
            log.info("Saved employee: {}", savedEmployee);
            redirectAttributes.addFlashAttribute("successMessage", "Сотрудник успешно добавлен.");
            return "redirect:/employee/" + savedEmployee.getId();
        } catch (DataIntegrityViolationException e) {
            log.error("Duplicate email: {}", employee.getEmail(), e);
            model.addAttribute("employee", employee);
            model.addAttribute("educations", educations);
            model.addAttribute("allSkills", Skills.values());
            model.addAttribute("categories", Skills.getAllCategories());
            model.addAttribute("errorMessage", "Ошибка: Email " + employee.getEmail() + " уже существует.");
            return "employee-edit";
        } catch (IllegalArgumentException e) {
            log.error("Invalid skill value", e);
            model.addAttribute("employee", employee);
            model.addAttribute("educations", educations);
            model.addAttribute("allSkills", Skills.values());
            model.addAttribute("categories", Skills.getAllCategories());
            model.addAttribute("errorMessage", "Ошибка: Неверное значение навыка.");
            return "employee-edit";
        } catch (Exception e) {
            log.error("Error saving employee", e);
            model.addAttribute("employee", employee);
            model.addAttribute("educations", educations);
            model.addAttribute("allSkills", Skills.values());
            model.addAttribute("categories", Skills.getAllCategories());
            model.addAttribute("errorMessage", "Ошибка сохранения сотрудника. Пожалуйста, попробуйте позже.");
            return "employee-edit";
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/employee/update/{id}")
    public String updateEmployee(
            @PathVariable Long id,
            @Valid @ModelAttribute Employee employee,
            BindingResult bindingResult,
            @RequestParam(value = "skills", required = false) List<String> skillNames,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "photoPath", required = false) String existingPhotoPath,
            @ModelAttribute("educations") List<Education> educations,
            RedirectAttributes redirectAttributes,
            Model model) {
        MDC.put("employeeId", id.toString());
        MDC.put("employeeEmail", employee.getEmail());
        try {
            if (bindingResult.hasErrors()) {
                model.addAttribute("employee", employee);
                model.addAttribute("educations", educations);
                model.addAttribute("allSkills", Skills.values());
                model.addAttribute("categories", Skills.getAllCategories());
                model.addAttribute("errorMessage", "Пожалуйста, исправьте ошибки в форме.");
                return "employee-edit";
            }
            Employee existingEmployee = employeeRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Неверный ID сотрудника: " + id));
            existingEmployee.setName(employee.getName());
            existingEmployee.setPhoneNumber(employee.getPhoneNumber());
            existingEmployee.setEmail(employee.getEmail());
            existingEmployee.setTelegram(employee.getTelegram());
            existingEmployee.setResume(employee.getResume());
            existingEmployee.setSkill(employee.getSkill());
            existingEmployee.setCategory(employee.getCategory());
            List<Skills> skills = processSkills(skillNames);
            existingEmployee.setSkills(skills);
            if (photo != null && !photo.isEmpty()) {
                if (!isValidImage(photo)) {
                    model.addAttribute("employee", employee);
                    model.addAttribute("educations", educations);
                    model.addAttribute("allSkills", Skills.values());
                    model.addAttribute("categories", Skills.getAllCategories());
                    model.addAttribute("errorMessage", "Недопустимый тип или размер файла. Разрешены JPEG, PNG, GIF до 5MB.");
                    return "employee-edit";
                }
                String filename = saveUploadedFile(photo);
                existingEmployee.setPhotoPath("/" + uploadDir + "/" + filename);
                log.info("Uploaded new photo: {}", filename);
            } else {
                existingEmployee.setPhotoPath(existingPhotoPath);
            }
            employeeRepository.save(existingEmployee);
            log.info("Deleting existing educations for employee: {}", id);
            educationRepository.deleteByEmployeeId(id);
            log.info("Saving new educations for employee: {}", id);
            saveEducations(educations, id);
            log.info("Updated employee: {}", existingEmployee);
            redirectAttributes.addFlashAttribute("successMessage", "Изменения сохранены.");
            return "redirect:/employee/" + id;
        } catch (DataIntegrityViolationException e) {
            log.error("Duplicate email: {}", employee.getEmail(), e);
            model.addAttribute("employee", employee);
            model.addAttribute("educations", educations);
            model.addAttribute("allSkills", Skills.values());
            model.addAttribute("categories", Skills.getAllCategories());
            model.addAttribute("errorMessage", "Ошибка: Email " + employee.getEmail() + " уже существует.");
            return "employee-edit";
        } catch (IllegalArgumentException e) {
            log.error("Invalid skill value", e);
            model.addAttribute("employee", employee);
            model.addAttribute("educations", educations);
            model.addAttribute("allSkills", Skills.values());
            model.addAttribute("categories", Skills.getAllCategories());
            model.addAttribute("errorMessage", "Ошибка: Неверное значение навыка.");
            return "employee-edit";
        } catch (Exception e) {
            log.error("Error updating employee", e);
            model.addAttribute("employee", employee);
            model.addAttribute("educations", educations);
            model.addAttribute("allSkills", Skills.values());
            model.addAttribute("categories", Skills.getAllCategories());
            model.addAttribute("errorMessage", "Ошибка обновления сотрудника. Пожалуйста, попробуйте позже.");
            return "employee-edit";
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes, Model model) {
        MDC.put("employeeId", id.toString());
        try {
            Employee employee = employeeRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Неверный ID сотрудника: " + id));
            educationRepository.deleteByEmployeeId(id);
            employeeRepository.delete(employee);
            log.info("Deleted employee: {}", id);
            redirectAttributes.addFlashAttribute("successMessage", "Сотрудник успешно удален.");
            return "redirect:/";
        } catch (Exception e) {
            log.error("Error deleting employee", e);
            model.addAttribute("errorMessage", "Ошибка при удалении сотрудника. Пожалуйста, попробуйте позже.");
            return "employees";
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/employee/check-email")
    @ResponseBody
    public Map<String, Boolean> checkEmail(@RequestParam String email) {
        MDC.put("email", email);
        try {
            boolean exists = employeeRepository.findByEmail(email).isPresent();
            return Collections.singletonMap("exists", exists);
        } finally {
            MDC.clear();
        }
    }

    private String saveUploadedFile(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get(uploadDir, fileName);
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());
        return fileName;
    }

    private boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String contentType = file.getContentType();
        return contentType != null &&
                ALLOWED_IMAGE_TYPES.contains(contentType) &&
                file.getSize() <= MAX_FILE_SIZE;
    }

    private List<Skills> processSkills(List<String> skillNames) {
        if (skillNames == null || skillNames.isEmpty()) {
            return new ArrayList<>();
        }
        return skillNames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(name -> !name.isEmpty())
                .map(name -> {
                    try {
                        return Skills.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid skill name: {}", name);
                        throw new IllegalArgumentException("Неверное значение навыка: " + name);
                    }
                })
                .collect(Collectors.toList());
    }

    private void saveEducations(List<Education> educations, Long employeeId) {
        if (educations != null && !educations.isEmpty()) {
            educations.forEach(edu -> {
                edu.setEmployeeId(employeeId);
                educationRepository.save(edu);
            });
            log.info("Saved {} educations for employee: {}", educations.size(), employeeId);
        }
    }
}