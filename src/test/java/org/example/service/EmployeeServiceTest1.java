package org.example.service;

import org.example.model.Employee;
import org.example.repository.EducationRepository;
import org.example.repository.EmployeeRepository;
import org.example.repository.EmployeeRepositoryJPA;
import org.example.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest1 {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EducationRepository educationRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private EmployeeRepositoryJPA employeeRepositoryJPA;

    private final int OFFSET = 1;
    private final int LIMIT = 10;
    private final Long EMPLOYEEID = 1L;
    private final double RATING = 1.0;

    @Test
    void findAll() {
        EmployeeService employeeService = new EmployeeService(employeeRepository, educationRepository, reviewRepository, employeeRepositoryJPA);
        List<Employee> employees = List.of(new Employee(), new Employee());
        when(employeeRepository.findAllPaginated(OFFSET, LIMIT)).thenReturn(employees);
        List<Employee> resaut = employeeService.findAll(OFFSET, LIMIT);
        assertNotNull(resaut);
        assertEquals(employees, resaut);
    }

    @Test
    void calculateAverageRating() {
        EmployeeService employeeService = new EmployeeService(employeeRepository, educationRepository, reviewRepository, employeeRepositoryJPA);
        when(reviewRepository.calculateAverageRating(EMPLOYEEID)).thenReturn(RATING);
        double result = employeeService.calculateAverageRating(EMPLOYEEID);
        assertEquals(RATING, result);
    }

}