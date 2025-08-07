package org.example.controller;

import org.example.model.Employee;
import org.example.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void listEmployees_ShouldReturnEmployeesPage() throws Exception {
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
}