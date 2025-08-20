package Poi.poi;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExcelWriter {
    public static void main(String[] args) {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream file = new FileOutputStream("output.xlsx")) {

            Sheet sheet = workbook.createSheet("Данные о пользователях");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Имя", "Возраст", "Город"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            Object[][] data = {
                    {"Иван", 25, "Брест"},
                    {"Мария", 30, "Минск"},
                    {"Петр", 35, "Могилев"}
            };

            for (int i = 0; i < data.length; i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue((String) data[i][0]);
                row.createCell(1).setCellValue((Integer) data[i][1]);
                row.createCell(2).setCellValue((String) data[i][2]);
            }

            workbook.write(file);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}