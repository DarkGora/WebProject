package org.example.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.example.dto.CallbackRequest;
import org.example.dto.EmployeeDto;
import org.example.request.CreateEmployeeRequest;
import org.example.request.FileFormat;
import org.example.request.SentFileRequest;
import org.example.service.EmailService;
import org.example.service.EmployeeServiceJPA;
import org.example.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/api")
public class Callbackcontroller {

    @Autowired
    private EmployeeServiceJPA employeeService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private FileService fileService;

    @Autowired
    private JavaMailSenderImpl mailSender;

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
    @PostMapping("/create-file/{id}")
    public ResponseEntity<String> createFile(@PathVariable Long id, @RequestParam FileFormat fileFormat) {
        try {
            EmployeeDto employeeDto = employeeService.getEmployeeById(id);
            ByteArrayOutputStream fileStream = fileService.createFile(employeeDto, fileFormat);
            return ResponseEntity.ok("File has been created successfully");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
        @PostMapping("/send/{id}")
    public ResponseEntity<String> sendEmail(@PathVariable Long id, @RequestBody @Valid SentFileRequest request) {
        try {
            EmployeeDto employee = employeeService.getEmployeeById(id);

            ByteArrayOutputStream fileStream = fileService.createFile(employee, request.getFileFormat());
            String fileName;
            String fileType;

            switch (request.getFileFormat()) {
                case DOCX:
                    fileName = "Резюме " + employee.getName() + ".docx";
                    fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    break;
                case EXEL:
                    fileName = "Данные " + employee.getName() + ".xlsx";
                    fileType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    break;
                default:
                    return ResponseEntity.badRequest().body("Неизвестный формат файла: " + request.getFileFormat());
            }

            emailService.sendEmailWithAttachment(
                    request.getEmail(),
                    "Резюме " + employee.getName(),
                    "Вы можете ознакомиться с резюме во вложении",
                    fileStream.toByteArray(),
                    fileName,
                    fileType
            );

            return ResponseEntity.ok("Email с данными " + employee.getName() + " отправлен");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Ошибка при отправке email: " + e.getMessage());
        }
    }
}