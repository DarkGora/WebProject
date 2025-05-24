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

import java.util.List;

/**
 * Контроллер для управления сотрудниками через веб-интерфейс.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    /**
     * Отображение списка сотрудников с пагинацией.
     *
     * @param page  номер страницы (начиная с 0)
     * @param model модель для передачи данных в шаблон
     * @return имя шаблона для списка сотрудников
     */
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
            log.error("Некорректные параметры пагинации для страницы {}: {}", page, e.getMessage());
            model.addAttribute("error", "Некорректные параметры пагинации");
        } catch (Exception e) {
            log.error("Ошибка при загрузке сотрудников для страницы {}: {}", page, e.getMessage());
            model.addAttribute("error", "Ошибка при загрузке списка сотрудников");
        }
        return "employees";
    }

    /**
     * Отображение формы для создания нового сотрудника.
     *
     * @param model модель для передачи данных в шаблон
     * @return имя шаблона для формы редактирования
     */
    @GetMapping("/employee/new")
    public String newEmployee(Model model) {
        model.addAttribute("employee", new Employee());
        log.debug("Открыта форма для нового сотрудника");
        return "employee-edit";
    }

    /**
     * Отображение формы для создания или редактирования сотрудника.
     *
     * @param id                 идентификатор сотрудника (опционально)
     * @param model              модель для передачи данных в шаблон
     * @param redirectAttributes атрибуты для перенаправления
     * @return имя шаблона или перенаправление
     */
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

    /**
     * Отображение детальной информации о сотруднике.
     *
     * @param id        идентификатор сотрудника
     * @param model     модель для передачи данных в шаблон
     * @param redirect  атрибуты для перенаправления
     * @return имя шаблона или перенаправление
     */
    @GetMapping("/employee/{id}")
    public String viewEmployee(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            Employee employee = employeeService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден с ID: " + id));
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

    /**
     * Сохранение или обновление сотрудника.
     *
     * @param employee  объект сотрудника
     * @param result    результат валидации
     * @param photo     файл фотографии (опционально)
     * @param redirect  атрибуты для перенаправления
     * @return перенаправление
     */
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
            Employee savedEmployee;
            if (employee.getId() == null) {
                log.info("Сохранение нового сотрудника: {}", employee.getName());
                savedEmployee = employeeService.save(employee, photo);
                redirect.addFlashAttribute("success", "Сотрудник успешно добавлен");
            } else {
                log.info("Обновление сотрудника с ID: {}", employee.getId());
                savedEmployee = employeeService.update(employee.getId(), employee, photo);
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

    /**
     * Удаление сотрудника.
     *
     * @param id        идентификатор сотрудника
     * @param redirect  атрибуты для перенаправления
     * @return перенаправление
     */
    @PostMapping("/employee/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            log.info("Удаление сотрудника с ID: {}", id);
            employeeService.delete(id);
            redirect.addFlashAttribute("success", "Сотрудник успешно удалён");
            return "redirect:";
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка при удалении сотрудника с ID {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:";
        } catch (Exception e) {
            log.error("Ошибка при удалении сотрудника с ID {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "Ошибка при удалении сотрудника");
            return "redirect:";
        }
    }
}