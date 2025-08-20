package Poi.poi;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.FileOutputStream;
import java.io.IOException;

public class DocWriter {
    public static void main(String[] args) {
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream file = new FileOutputStream("output1.docx")) {
            XWPFParagraph paragraph = doc.createParagraph();
            paragraph.setAlignment(ParagraphAlignment.CENTER);

            XWPFRun run = paragraph.createRun();
            run.setText("Texnolojia");
            run.addBreak();
            run.setText("Marina");

            run.setBold(true);
            run.setFontFamily("Verdana");
            run.setFontSize(12);
            run.addBreak();

            XWPFParagraph paragraph2 = doc.createParagraph();
            paragraph2.setAlignment(ParagraphAlignment.LEFT);

            XWPFRun run2 = paragraph2.createRun();
            run2.setText("Проделки");
            run2.addBreak();
            run2.setText("Документ успешно создан!");
            run2.setFontSize(14);


            doc.write(file);
            System.out.println("Создан:output1.docx");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

