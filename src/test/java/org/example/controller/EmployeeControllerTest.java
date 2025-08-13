package org.example.controller;

import org.example.model.Employee;
import org.example.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void listEmployees() throws Exception {
        Employee mockEmployee = Employee.builder()
                .id(1L)
                .name("Иван")
                .active(true)
                .build();
        List<Employee> mockEmployees = List.of(mockEmployee);
        when(employeeService.findAll(anyInt(), anyInt())).thenReturn(mockEmployees);
        when(employeeService.count()).thenReturn(1L);
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees"))
                .andExpect(model().attributeExists("employees"));
    }

    @Test
    void saveEmployee() throws Exception {
        mockMvc.perform(multipart("/employee/save"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-edit"));
    }

    @Test
    void save_Employee() throws Exception {
        Employee mockEmployee = Employee.builder()
                .id(1L)
                .name("Иван")
                .active(true)
                .build();
        when(employeeService.save(any(Employee.class), anyString())).thenReturn(mockEmployee);

        mockMvc.perform(post("/employee/save")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Иван")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-edit")); // или другое ожидаемое представление
    }
    @Test
    void listEmployee_1() throws Exception {
        Employee employee1 = Employee.builder().id(1L).name("Иван").active(true).build();
        Employee employee2 = Employee.builder().id(2L).name("Мария").active(false).build();

        when(employeeService.findAll(anyInt(), anyInt())).thenReturn(List.of(employee1,employee2));
        when(employeeService.count()).thenReturn(2L);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("employees"))
                .andExpect(view().name("employees"))
                .andExpect(model().attribute("employees", hasSize(2)))
                .andExpect(model().attribute("totalEmployees", 2l));
    }
}