package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Employee;
import org.example.model.dto.ProfileUpdateRequest;
import org.example.service.EmployeeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final EmployeeService employeeService;

    @GetMapping
    public String profilePage(Model model, @AuthenticationPrincipal OidcUser user) {
        log.debug("=== PROFILE PAGE DEBUG ===");

        if (user == null) {
            log.error("OidcUser is NULL - user not authenticated");
            return "redirect:/login";
        }

        // Отладочная информация
        log.debug("OIDC User details:");
        log.debug("  Name: {}", user.getName());
        log.debug("  Email: {}", user.getEmail());
        log.debug("  Preferred Username: {}", user.getPreferredUsername());
        log.debug("  Claims: {}", user.getClaims().keySet());

        String email = user.getEmail();
        log.debug("Searching employee by email: {}", email);

        try {
            Optional<Employee> employeeOpt = employeeService.findByEmail(email);

            if (employeeOpt.isPresent()) {
                Employee employee = employeeOpt.get();
                log.debug("Employee found: {} (ID: {})", employee.getName(), employee.getId());
                model.addAttribute("employee", employee);
            } else {
                log.warn("Employee not found for email: {}", email);
                Employee tempEmployee = createTempEmployee(user);
                model.addAttribute("employee", tempEmployee);
                model.addAttribute("warning", "Профиль не найден в базе данных");
            }

            model.addAttribute("pageTitle", "Мой профиль");
            model.addAttribute("oidcUser", user);

        } catch (Exception e) {
            log.error("Error loading profile: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка загрузки профиля: " + e.getMessage());
        }

        log.debug("=== END PROFILE DEBUG ===");
        return "profile";
    }

    private Employee createTempEmployee(OidcUser user) {
        return Employee.builder()
                .name(user.getPreferredUsername() != null ? user.getPreferredUsername() : user.getName())
                .email(user.getEmail())
                .position("Пользователь системы")
                .department("Не назначен")
                .active(true)
                .emailNotifications(true)
                .smsNotifications(false)
                .theme("dark")
                .language("ru")
                .itemsPerPage(10)
                .build();
    }

    @PostMapping("/update")
    public String updateProfile(@ModelAttribute ProfileUpdateRequest request,
                                @AuthenticationPrincipal OidcUser user,
                                RedirectAttributes redirectAttributes) {
        String email = user.getEmail();
        log.info("Updating profile for email: {}", email);

        try {
            employeeService.updateEmployeeProfile(email, request);
            redirectAttributes.addFlashAttribute("success", "Профиль успешно обновлен");
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении профиля: " + e.getMessage());
        }

        return "redirect:/profile";
    }
}