package org.example.service;


import org.example.repository.EducationRepository;
import org.example.repository.EmployeeRepository;
import org.example.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


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


    private final int OFFSET = 1;
    private final int LIMIT = 10;
    private final Long EMPLOYEEID = 1L;
    private final double RATING = 1.0;

    /*@Test
    void findAll() {
        EmployeeService employeeService = new EmployeeService(employeeRepository, educationRepository, reviewRepository);
        List<Employee> employees = List.of(new Employee(), new Employee());
        when(employeeRepository.findAllById()).thenReturn(employees);
        List<Employee> resaut = employeeService.findAll(OFFSET, LIMIT);
        assertNotNull(resaut);
        assertEquals(employees, resaut);
    }*/

    @Test
    void calculateAverageRating() {
        EmployeeService employeeService = new EmployeeService(employeeRepository, educationRepository, reviewRepository);
        when(reviewRepository.calculateAverageRating(EMPLOYEEID)).thenReturn(RATING);
        double result = employeeService.calculateAverageRating(EMPLOYEEID);
        assertEquals(RATING, result);
    }

}