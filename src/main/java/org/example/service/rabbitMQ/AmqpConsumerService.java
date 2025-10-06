package org.example.service.rabbitMQ;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.RabbitConfig;
import org.example.model.dto.EmployeeDto;
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
        log.info("Получен сотрудник для отправки email: {}", employee.getName());

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
            log.error("Ошибка при создании файла для сотрудника {}: {}", employee.getName(), e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при отправке email для сотрудника {}: {}", employee.getName(), e.getMessage());
        }
    }


    @RabbitListener(queues = RabbitConfig.BAGiS_QUEUE)
    public void receiveExcelMessage(EmployeeDto employee) {
        System.out.println("Go to Excel to email ot: " + employee);

        try {
            ByteArrayOutputStream outputStream = fileService.createFile(employee, FileFormat.EXEL);
            byte[] fileContent = outputStream.toByteArray();
            String fileName = employee.getName() + "_resume.xlsx";

            emailService.sendEmailWithAttachment(
                    "andrey.evushvggtjo@gmail.com",
                    "Резюме в Excel " + employee.getName(),
                    "В приложении Excel fiels с resume сотрудника",
                    fileContent,
                    fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );

            log.info("Email успешно ушёл: {}", employee.getName());

        } catch (IOException e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
            e.printStackTrace();
        }
    }
}