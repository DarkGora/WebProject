package org.example.fileFabrica;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.example.dto.EmployeeDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DocxFileGenerator implements FileGenerator {

    @Override
    public ByteArrayOutputStream generateFile(EmployeeDto employee) throws IOException {
        XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            XWPFParagraph titleParagraph = doc.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setFontSize(16);
            titleRun.setBold(true);
            titleRun.setFontFamily("Verdana");
            titleRun.setText("Резюме: " + employee.getName());
            titleRun.addBreak();

            XWPFParagraph infoParagraph = doc.createParagraph();
            infoParagraph.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun infoRun = infoParagraph.createRun();
            infoRun.setFontSize(12);
            infoRun.setFontFamily("Verdana");

            infoRun.setText("Имя: " + employee.getName());
            infoRun.addBreak();
            if (employee.getEmail() != null) {
                infoRun.setText("Email: " + employee.getEmail());
                infoRun.addBreak();
            }

            doc.write(outputStream);
        } finally {
            doc.close();
        }

        return outputStream;
    }
}