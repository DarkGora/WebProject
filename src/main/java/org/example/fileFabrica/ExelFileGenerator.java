package org.example.fileFabrica;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.dto.EmployeeDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ExelFileGenerator implements FileGenerator {

    @Override
    public ByteArrayOutputStream generateFile(EmployeeDto employee) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            Sheet sheet = workbook.createSheet("Данные сотрудника");

            Row headerRow = sheet.createRow(0);
            Cell headerCell = headerRow.createCell(0);
            headerCell.setCellValue("Данные сотрудника: " + employee.getName());

            int rowNum = 2;
            String[][] employeeData = {
                    {"Имя", employee.getName()},
                    {"Email", employee.getEmail() != null ? employee.getEmail() : ""},
                    {"Телефон", employee.getPhone() != null ? employee.getPhone() : ""},
                    {"Должность", employee.getPosition() != null ? employee.getPosition() : ""}
            };

            for (String[] data : employeeData) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(data[0]);
                row.createCell(1).setCellValue(data[1]);
            }

            workbook.write(outputStream);
        } finally {
            workbook.close();
        }

        return outputStream;
    }
}