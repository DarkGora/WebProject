package org.example.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.example.dto.EmployeeDto;
import org.example.mappers.EmployeeMapper;
import org.example.model.Employee;
import org.example.repository.EmployeeRepositoryDto;

import org.example.request.CreateEmployeeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class EmployeeServiceJPA {
    private final EmployeeRepositoryDto repository;
    private final EmployeeMapper mapper;

    public EmployeeDto getEmployeeById(Long id) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));

        return mapper.toDto(employee);
    }

    public List<EmployeeDto> getAllEmployee() {
        List<Employee> employee = repository.findAll();
        return mapper.toDto(employee);
    }

    @Transactional
    public EmployeeDto createEmployee(CreateEmployeeRequest request) {
        Employee employee = Employee.builder()
                .name(request.getName())
                .build();
        var  saved = repository.save(employee);
        return mapper.toDto(saved);
    }

    public void deleteEmployee(Long id) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        repository.delete(employee);
    }
}

