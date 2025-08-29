package org.example.service.rabbitMQ;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.RabbitConfig;
import org.example.dto.EmployeeDto;
import org.example.fileFabrica.FileFormat;
import org.example.service.EmailService;
import org.example.service.FileService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
@Slf4j
@Service
@AllArgsConstructor
public class AmqpConsumerService {
    private final EmailService emailService;
    private final FileService fileService;

    @RabbitListener(queues = RabbitConfig.MAIL_QUEUE)
    public void receiveMessage(EmployeeDto employee) {
        System.out.println("Получен сотрудник: " + employee);

        try {
            ByteArrayOutputStream outputStream = fileService.createFile(employee, FileFormat.DOCX);
            byte[] fileContent = outputStream.toByteArray();
            String fileName = employee.getName() + "_resume.docx";

            emailService.sendEmailWithAttachment(
                    "andrey.evushvggtjo@gmail.com",
                    "Резюме " + employee.getName(),
                    "Вы можете ознакомиться с содержимым письма",
                    fileContent,
                    fileName,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );

            log.info("Email с резюме отправлен для сотрудника: {}", employee.getName());

        } catch (IOException e) {
            System.err.println("Ошибка при создании файла: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Ошибка при отправке email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}