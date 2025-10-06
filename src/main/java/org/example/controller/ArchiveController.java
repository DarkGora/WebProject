package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Employee;
import org.example.service.EmployeeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('resume.admin')")
public class ArchiveController {

    private final EmployeeService employeeService;

    // ИСПРАВЛЕНО: убрал дублирующий /admin в пути
    @GetMapping("/deleted")
    public String viewDeletedEmployees(Model model) {
        try {
            List<Employee> deletedEmployees = employeeService.findDeletedEmployees();
            List<String> departments = employeeService.findAllDistinctDepartments();
            List<String> positions = employeeService.findAllDistinctPositions();

            model.addAttribute("deletedEmployees", deletedEmployees);
            model.addAttribute("totalDeleted", deletedEmployees.size());
            model.addAttribute("departments", departments);
            model.addAttribute("positions", positions);

            log.debug("Loaded {} deleted employees", deletedEmployees.size());
            return "admin/deleted-employees"; // ИСПРАВЛЕНО: правильный путь к шаблону

        } catch (Exception e) {
            log.error("Ошибка при загрузке архива сотрудников: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка при загрузке архива");
            return "admin/deleted-employees";
        }
    }

    // ИСПРАВЛЕНО: правильный путь для restore
    @PostMapping("/restore/{id}")
    public String restoreEmployee(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            log.info("Восстановление сотрудника с ID: {}", id);
            employeeService.restore(id);
            redirect.addFlashAttribute("success", "Сотрудник успешно восстановлен");
            return "redirect:/employee/" + id;
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка при восстановлении сотрудника с ID {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/deleted";
        } catch (Exception e) {
            log.error("Ошибка при восстановлении сотрудника с ID {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "Ошибка при восстановлении сотрудника");
            return "redirect:/admin/deleted";
        }
    }

    // ИСПРАВЛЕНО: правильный путь для permanent-delete
    @PostMapping("/permanent-delete/{id}")
    public String permanentDelete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Полное удаление сотрудника из архива ID: {}", id);

        try {
            employeeService.permanentDelete(id);
            redirectAttributes.addFlashAttribute("success", "Сотрудник полностью удален из системы");
            log.info("Сотрудник ID {} полностью удален из системы", id);
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка при полном удалении сотрудника ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при полном удалении сотрудника ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении сотрудника");
        }

        return "redirect:/admin/deleted";
    }
}