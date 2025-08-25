package org.example.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.example.dto.CallbackRequest;
import org.example.dto.EmployeeDto;
import org.example.request.CreateEmployeeRequest;
import org.example.service.EmailService;
import org.example.service.EmployeeServiceJPA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.net.URI;

@RestController
@RequestMapping("/api")
public class Callbackcontroller {

    @Autowired
    private EmployeeServiceJPA employeeService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestBody CallbackRequest request) {
        try {
            ByteArrayOutputStream docFile = null;
            ByteArrayOutputStream excelFile = null;

            if (request.isSendDoc()) {
                docFile = emailService.createWordDocument(request);
            }

            if (request.isSendExcel()) {
                excelFile = emailService.createExcelReport(request);
            }

            emailService.sendCallbackEmail(request, docFile, excelFile);

            return ResponseEntity.ok("Заявка принята и email отправлен");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Ошибка: " + e.getMessage());
        }
    }

    @PostMapping("/creaty")
    public ResponseEntity<EmployeeDto> createEmployee(
                                                       @Parameter(description = "Данные сотрудника", required = true)
                                                       @RequestBody @Valid CreateEmployeeRequest request) {
        try {
            EmployeeDto createdEmployee = employeeService.createEmployee(request);
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(createdEmployee.getId())
                    .toUri();
            return ResponseEntity.created(location).body(createdEmployee);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}