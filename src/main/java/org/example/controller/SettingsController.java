package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Employee;
import org.example.service.EmployeeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final EmployeeService employeeService;

    @GetMapping
    public String settingsPage(Model model, @AuthenticationPrincipal OidcUser user) {
        log.debug("Loading settings page for user: {}", user.getEmail());

        try {
            String email = user.getEmail();
            Employee employee = employeeService.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден"));

            model.addAttribute("employee", employee);
            model.addAttribute("pageTitle", "Настройки");

        } catch (Exception e) {
            log.error("Error loading settings: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка загрузки настроек: " + e.getMessage());
        }

        return "settings";
    }

    @PostMapping("/update")
    public String updateSettings(@ModelAttribute Employee employee,
                                 @AuthenticationPrincipal OidcUser user,
                                 RedirectAttributes redirectAttributes) {
        String email = user.getEmail();
        log.info("Updating settings for: {}", email);

        try {
            employeeService.updateEmployeeSettings(email, employee);
            redirectAttributes.addFlashAttribute("success", "Настройки успешно обновлены");
        } catch (Exception e) {
            log.error("Error updating settings: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении настроек: " + e.getMessage());
        }

        return "redirect:/settings";
    }
}