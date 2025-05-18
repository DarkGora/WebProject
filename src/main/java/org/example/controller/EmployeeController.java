package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Employee;
import org.example.service.EmployeeService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    @GetMapping("/")
    @Transactional(readOnly = true)
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
        } catch (Exception e) {
            log.error("Ошибка при загрузке сотрудников для страницы {}: {}", page, e.getMessage(), e);
            model.addAttribute("error", "Ошибка при загрузке списка сотрудников: " + e.getMessage());
        }
        return "employees";
    }

    @GetMapping("/employee/new")
    @Transactional(readOnly = true)
    public String newEmployee(Model model) {
        model.addAttribute("employee", new Employee());
        log.debug("Открыта форма для нового сотрудника");
        return "employee-edit";
    }

    @GetMapping({"/employee/add", "/employee/edit/{id}"})
    @Transactional(readOnly = true)
    public String editEmployee(@PathVariable(required = false) Long id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            Employee employee;

            if (id != null) {
                log.info("Запрос на редактирование сотрудника с ID: {}", id);
                Optional<Employee> employeeOpt = employeeService.findById(id);

                if (employeeOpt.isEmpty()) {
                    log.warn("Сотрудник с ID {} не найден", id);
                    redirectAttributes.addFlashAttribute("error", "Сотрудник не найден");
                    return "redirect:/employees";
                }

                employee = employeeOpt.get();
            } else {
                log.info("Запрос на создание нового сотрудника");
                employee = new Employee();
                // Инициализация полей при необходимости
                employee.setEducations(new ArrayList<>());
            }

            model.addAttribute("employee", employee);
            return "employee-edit";

        } catch (Exception e) {
            log.error("Ошибка при открытии формы сотрудника", e);
            redirectAttributes.addFlashAttribute("error", "Произошла ошибка: " + e.getMessage());
            return "redirect:/employees";
        }
    }

    @GetMapping("/employee/{id}")
    @Transactional(readOnly = true)
    public String viewEmployee(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Optional<Employee> employee = employeeService.findById(id);
        if (employee.isPresent()) {
            model.addAttribute("employee", employee.get());
            return "employee-details";
        }
        redirect.addFlashAttribute("error", "Сотрудник с ID " + id + " не найден");
        log.warn("Сотрудник с ID {} не найден", id);
        return "redirect:/";
    }

    @PostMapping("/employee/save")
    public String saveEmployee(@Valid @ModelAttribute("employee") Employee employee,
                               BindingResult result,
                               @RequestParam(value = "photo", required = false) MultipartFile photo,
                               RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "employee-edit";
        }

        try {
            Employee savedEmployee;
            if (employee.getId() == null) {
                savedEmployee = employeeService.save(employee, photo);
                redirect.addFlashAttribute("success", "Сотрудник успешно добавлен");
            } else {
                savedEmployee = employeeService.update(employee.getId(), employee, photo);
                redirect.addFlashAttribute("success", "Сотрудник успешно обновлён");
            }
            return "redirect:/employee/" + savedEmployee.getId(); // Перенаправляем на страницу сотрудника
        } catch (Exception e) {
            log.error("Ошибка сохранения сотрудника", e);
            redirect.addFlashAttribute("error", "Ошибка сохранения: " + e.getMessage());
            return "redirect:/employee/add"; // Возвращаем на форму добавления при ошибке
        }
    }

    @PostMapping("/employee/update/{id}")
    @Transactional
    public String updateEmployee(@PathVariable Long id,
                                 @Valid @ModelAttribute("employee") Employee employee,
                                 BindingResult result,
                                 @RequestParam("photo") MultipartFile photo,
                                 RedirectAttributes redirect,
                                 Model model) {
        if (result.hasErrors()) {
            log.debug("Ошибки валидации при обновлении сотрудника с ID {}: {}", id, result.getAllErrors());
            model.addAttribute("employee", employee);
            return "employee-edit";
        }

        try {
            if (!employeeService.findById(id).isPresent()) {
                log.error("Сотрудник с ID {} не найден", id);
                redirect.addFlashAttribute("error", "Сотрудник с ID " + id + " не найден");
                return "redirect:/";
            }
            employee.setId(id);
            employeeService.save(employee, photo);
            redirect.addFlashAttribute("success", "Сотрудник успешно обновлён!");
            log.info("Успешно обновлён сотрудник с ID: {}", id);
        } catch (Exception e) {
            log.error("Ошибка при обновлении сотрудника с ID {}: {}", id, e.getMessage(), e);
            result.reject("global", "Ошибка при обновлении сотрудника: " + e.getMessage());
            model.addAttribute("employee", employee);
            return "employee-edit";
        }
        return "redirect:/employee/" + id;
    }

    @PostMapping("/employee/delete/{id}")
    @Transactional
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            if (!employeeService.findById(id).isPresent()) {
                log.error("Сотрудник с ID {} не найден для удаления", id);
                redirect.addFlashAttribute("error", "Сотрудник с ID " + id + " не найден");
                return "redirect:/";
            }
            employeeService.delete(id);
            redirect.addFlashAttribute("success", "Сотрудник успешно удалён!");
            log.info("Успешно удалён сотрудник с ID: {}", id);
        } catch (Exception e) {
            log.error("Ошибка при удалении сотрудника с ID {}: {}", id, e.getMessage(), e);
            redirect.addFlashAttribute("error", "Ошибка при удалении сотрудника: " + e.getMessage());
            return "redirect:/";
        }
        return "redirect:/";
    }
}