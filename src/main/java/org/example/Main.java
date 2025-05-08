package org.example;

import org.example.model.Employee;
import org.example.model.Skills;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@Controller
public class Main {
    private static final String UPLOAD_DIR = "uploads";
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_FILE_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".gif");

    private final EmployeeRepository employeeRepository;

    public Main(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @GetMapping("/")
    public String showEmployees(Model model,
                                @RequestParam(required = false) String search,
                                @RequestParam(required = false) String categoryFilter) {
        List<Employee> employees = employeeRepository.findAll();

        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            employees = employees.stream()
                    .filter(e -> e.getName().toLowerCase().contains(searchLower) ||
                            e.getEmail().toLowerCase().contains(searchLower) ||
                            (e.getPhoneNumber() != null && e.getPhoneNumber().contains(search)))
                    .collect(Collectors.toList());
        }

        if (categoryFilter != null && !categoryFilter.isEmpty()) {
            employees = employees.stream()
                    .filter(e -> e.getSkills().stream()
                            .anyMatch(skill -> skill.getCategory().equalsIgnoreCase(categoryFilter)))
                    .collect(Collectors.toList());
        }

        employees.sort(Comparator.comparing(Employee::getName));

        model.addAttribute("employees", employees);
        model.addAttribute("allSkills", Skills.values());
        model.addAttribute("categories", Skills.getAllCategories());
        return "employees";
    }

    @PostMapping("/employee/{id}/upload-photo")
    public String uploadPhoto(@PathVariable Long id,
                              @RequestParam("photo") MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Файл не выбран");
            return "redirect:/employee/edit/" + id;
        }

        if (!isValidImage(file)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Недопустимый тип или размер файла. Разрешены JPEG, PNG, GIF до 5MB");
            return "redirect:/employee/edit/" + id;
        }

        try {
            String fileName = saveUploadedFile(file);
            Employee employee = employeeRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Неверный ID сотрудника: " + id));

            if (employee.getPhotoPath() != null) {
                deleteOldPhoto(employee.getPhotoPath());
            }

            employee.setPhotoPath("/" + UPLOAD_DIR + "/" + fileName);
            employeeRepository.save(employee);

            redirectAttributes.addFlashAttribute("successMessage", "Фото успешно загружено");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при загрузке файла: " + e.getMessage());
        }

        return "redirect:/employee/edit/" + id;
    }

    @PostMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            employeeRepository.findById(id).ifPresent(employee -> {
                if (employee.getPhotoPath() != null) {
                    deleteOldPhoto(employee.getPhotoPath());
                }
                employeeRepository.deleteById(employee.getId());
                redirectAttributes.addFlashAttribute("successMessage", "Сотрудник успешно удален");
            });
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении: " + e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/employee/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Неверный ID сотрудника: " + id));

        model.addAttribute("employee", employee);
        model.addAttribute("allSkills", Skills.values());
        model.addAttribute("categories", Skills.getAllCategories());
        return "employee-edit";
    }

    @PostMapping("/employee/update/{id}")
    public String updateEmployee(@PathVariable Long id,
                                 @ModelAttribute Employee employee,
                                 @RequestParam(value = "selectedSkills", required = false) List<String> selectedSkills,
                                 RedirectAttributes redirectAttributes) {
        try {
            Employee existingEmployee = employeeRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Неверный ID сотрудника: " + id));

            existingEmployee.setName(employee.getName());
            existingEmployee.setPhoneNumber(employee.getPhoneNumber());
            existingEmployee.setEmail(employee.getEmail());
            existingEmployee.setTelegram(employee.getTelegram());
            existingEmployee.setResume(employee.getResume());
            existingEmployee.setSchool(employee.getSchool());

            if (selectedSkills != null) {
                List<Skills> skills = selectedSkills.stream()
                        .map(Skills::valueOf)
                        .collect(Collectors.toList());
                existingEmployee.setSkills(skills);
            } else {
                existingEmployee.setSkills(new ArrayList<>());
            }

            employeeRepository.save(existingEmployee);
            redirectAttributes.addFlashAttribute("successMessage", "Изменения сохранены");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении: " + e.getMessage());
            return "redirect:/employee/edit/" + id;
        }

        return "redirect:/employee/" + id;
    }

    @GetMapping("/employee/{id}")
    public String showEmployee(@PathVariable Long id, Model model) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Неверный ID сотрудника: " + id));
        model.addAttribute("employee", employee);
        return "employee-details";
    }

    @GetMapping("/employee/new")
    public String showNewEmployeeForm(Model model) {
        model.addAttribute("employee", new Employee());
        model.addAttribute("allSkills", Skills.values());
        model.addAttribute("categories", Skills.getAllCategories());
        return "employee-edit";
    }

    @PostMapping("/employee/save")
    public String saveEmployee(@ModelAttribute Employee employee,
                               @RequestParam(value = "selectedSkills", required = false) List<String> selectedSkills,
                               @RequestParam("photo") MultipartFile photo,
                               RedirectAttributes redirectAttributes) {
        try {
            if (selectedSkills != null) {
                List<Skills> skills = selectedSkills.stream()
                        .map(Skills::valueOf)
                        .collect(Collectors.toList());
                employee.setSkills(skills);
            }

            if (!photo.isEmpty()) {
                if (!isValidImage(photo)) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Недопустимый тип или размер файла. Разрешены JPEG, PNG, GIF до 5MB");
                    return "redirect:/employee/new";
                }
                String fileName = saveUploadedFile(photo);
                employee.setPhotoPath("/" + UPLOAD_DIR + "/" + fileName);
            }

            Employee savedEmployee = employeeRepository.save(employee);
            redirectAttributes.addFlashAttribute("successMessage", "Сотрудник успешно добавлен");
            return "redirect:/employee/" + savedEmployee.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при сохранении: " + e.getMessage());
            return "redirect:/employee/new";
        }
    }

    private String saveUploadedFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();

        if (!ALLOWED_FILE_EXTENSIONS.contains(fileExtension)) {
            throw new IllegalArgumentException("Недопустимое расширение файла");
        }

        String fileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        return fileName;
    }

    private void deleteOldPhoto(String photoPath) {
        try {
            if (photoPath != null && photoPath.startsWith("/" + UPLOAD_DIR + "/")) {
                String filename = photoPath.substring(photoPath.lastIndexOf("/") + 1);
                Path filePath = Paths.get(UPLOAD_DIR, filename);
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при удалении файла: " + e.getMessage());
        }
    }

    private boolean isValidImage(MultipartFile file) {
        return file.getSize() <= MAX_FILE_SIZE &&
                file.getContentType() != null &&
                ALLOWED_IMAGE_TYPES.contains(file.getContentType()) &&
                file.getOriginalFilename() != null &&
                ALLOWED_FILE_EXTENSIONS.contains(
                        file.getOriginalFilename().substring(
                                file.getOriginalFilename().lastIndexOf(".")).toLowerCase());
    }

    @Configuration
    public static class WebConfig implements WebMvcConfigurer {
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/" + UPLOAD_DIR + "/**")
                    .addResourceLocations("file:" + UPLOAD_DIR + "/")
                    .setCachePeriod(3600);

            registry.addResourceHandler("/static/**")
                    .addResourceLocations("classpath:/static/")
                    .setCachePeriod(3600);
        }
    }
}