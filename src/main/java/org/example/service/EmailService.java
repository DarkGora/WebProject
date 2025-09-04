package org.example.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.example.dto.CallbackRequest;
import org.example.dto.EmployeeDto;
import org.example.fileFabrica.FileFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    public void sendCallbackEmail(CallbackRequest request, ByteArrayOutputStream docFile, ByteArrayOutputStream excelFile) throws MessagingException, IOException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo("andrey.evushvggtjo@gmail.com");
            helper.setSubject("Сообщение от " + request.getName());
            helper.setFrom("dima.pulik1488@gmail.com");

            StringBuilder emailContent = new StringBuilder();
            emailContent.append("<h3>Новое сообщение</h3>")
                    .append("<p><strong>Имя:</strong> ").append(request.getName()).append("</p>");

            if (request.getEmail() != null) {
                emailContent.append("<p><strong>Email:</strong> ").append(request.getEmail()).append("</p>");
            }
            if (request.getMessage() != null) {
                emailContent.append("<p><strong>Сообщение:</strong> ").append(request.getMessage()).append("</p>");
            }

            // Добавляем вложения если есть
            boolean hasAttachments = false;
            if (docFile != null && docFile.size() > 0) {
                addAttachment(helper, docFile.toByteArray(),
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "Сообщение.docx", "Документ заявки");
                hasAttachments = true;
            }

            if (excelFile != null && excelFile.size() > 0) {
                addAttachment(helper, excelFile.toByteArray(),
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "Отчет.xlsx", "Excel отчет");
                hasAttachments = true;
            }

            if (hasAttachments) {
                emailContent.append("<p><strong>Вложения:</strong> ");
                if (docFile != null && docFile.size() > 0) emailContent.append("Word документ, ");
                if (excelFile != null && excelFile.size() > 0) emailContent.append("Excel отчет");
                emailContent.append("</p>");
            }

            helper.setText(emailContent.toString(), true);
            mailSender.send(message);
            logger.info("Email успешно отправлен для заявки от {} с {} вложениями",
                    request.getName(), countAttachments(docFile, excelFile));

        } catch (MessagingException e) {
            logger.error("Ошибка при отправке email: {}", e.getMessage(), e);
            throw new MessagingException("Ошибка отправки email", e);
        }
    }

    private void addAttachment(MimeMessageHelper helper, byte[] fileData, String contentType, String fileName, String description) throws MessagingException {
        try {
            InputStreamSource attachmentSource = new ByteArrayResource(fileData);
            helper.addAttachment(fileName, attachmentSource, contentType);
            logger.debug("Добавлено вложение: {} ({})", fileName, description);
        } catch (MessagingException e) {
            logger.error("Ошибка при добавлении вложения {}: {}", fileName, e.getMessage());
            throw e;
        }
    }

    public void sendEmailWithAttachment(String toEmail, String subject, String text,
                                        byte[] attachment, String fileName, String contentType)
            throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setFrom("dima.pulik1488@gmail.com");
            helper.setText(text);

            if (attachment != null && attachment.length > 0) {
                InputStreamSource attachmentSource = new ByteArrayResource(attachment);
                helper.addAttachment(fileName, attachmentSource, contentType);
            }

            mailSender.send(message);
            logger.info("Email с вложением успешно отправлен на {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Ошибка при отправке email с вложением: {}", e.getMessage(), e);
            throw new MessagingException("Ошибка отправки email с вложением", e);
        }
    }

    private int countAttachments(ByteArrayOutputStream... files) {
        int count = 0;
        for (ByteArrayOutputStream file : files) {
            if (file != null && file.size() > 0) {
                count++;
            }
        }
        return count;
    }

    public ByteArrayOutputStream createWordDocument(CallbackRequest request) throws IOException {
        XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            XWPFParagraph titleParagraph = doc.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setFontSize(16);
            titleRun.setBold(true);
            titleRun.setFontFamily("Verdana");
            titleRun.setText("Заявка от: " + request.getName());
            titleRun.addBreak();

            XWPFParagraph infoParagraph = doc.createParagraph();
            infoParagraph.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun infoRun = infoParagraph.createRun();
            infoRun.setFontSize(12);
            infoRun.setFontFamily("Verdana");

            infoRun.setText("Имя: " + request.getName());
            infoRun.addBreak();
            if (request.getEmail() != null) {
                infoRun.setText("Email: " + request.getEmail());
                infoRun.addBreak();
            }
            if (request.getMessage() != null) { // ИСПРАВЛЕНО: getMessage() вместо getName()
                infoRun.setText("Сообщение: " + request.getMessage());
                infoRun.addBreak();
            }

            doc.write(outputStream);
        } finally {
            doc.close();
        }

        return outputStream;
    }

    public ByteArrayOutputStream createExcelReport(CallbackRequest request) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            Sheet sheet = workbook.createSheet("Данные о пользователях");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Имя", "Email", "Сообщение"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(request.getName());
            dataRow.createCell(1).setCellValue(request.getEmail() != null ? request.getEmail() : "");
            dataRow.createCell(2).setCellValue(request.getMessage() != null ? request.getMessage() : ""); // ИСПРАВЛЕНО

            workbook.write(outputStream);
        } finally {
            workbook.close();
        }

        return outputStream;
    }
}