package org.example.fileFabrica;

import org.example.dto.EmployeeDto;
import org.example.request.FileFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class FileGeneratorFactory {
    public static FileGenerator getFileGenerator(FileFormat fileFormat) {
        return switch (fileFormat) {
            case DOCX -> new DocxFileGenerator();
            case EXEL -> new ExelFileGenerator();
            default -> throw new IllegalArgumentException("Неизвестный формат файла: " + fileFormat);
        };
    }
}